package com.sedmelluq.discord.lavaplayer.player;

import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.ProbingAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.*;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpConfigurable;
import com.sedmelluq.discord.lavaplayer.tools.io.MessageInput;
import com.sedmelluq.discord.lavaplayer.tools.io.MessageOutput;
import com.sedmelluq.discord.lavaplayer.track.*;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioTrackExecutor;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;
import com.sedmelluq.lava.common.tools.DaemonThreadFactory;
import com.sedmelluq.lava.common.tools.ExecutorTools;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.FAULT;
import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;

/**
 * The default implementation of audio player manager.
 */
public class DefaultAudioPlayerManager implements AudioPlayerManager {
    private static final int TRACK_INFO_VERSIONED = 1;
    private static final int TRACK_INFO_VERSION = 3;

    private static final int DEFAULT_FRAME_BUFFER_DURATION = (int) TimeUnit.SECONDS.toMillis(5);
    private static final int DEFAULT_CLEANUP_THRESHOLD = (int) TimeUnit.MINUTES.toMillis(1);

    private static final int MAXIMUM_LOAD_REDIRECTS = 5;
    private static final int DEFAULT_LOADER_POOL_SIZE = 10;
    private static final int LOADER_QUEUE_CAPACITY = 5000;

    private static final Logger log = LoggerFactory.getLogger(DefaultAudioPlayerManager.class);

    private final List<AudioSourceManager> sourceManagers;
    private volatile Function<RequestConfig, RequestConfig> httpConfigurator;
    private volatile Consumer<HttpClientBuilder> httpBuilderConfigurator;

    // Executors
    private final ExecutorService trackPlaybackExecutorService;
    private final ThreadPoolExecutor trackInfoExecutorService;
    private final ScheduledExecutorService scheduledExecutorService;
    private final OrderedExecutor orderedInfoExecutor;

    // Configuration
    private volatile long trackStuckThreshold;
    private volatile AudioConfiguration configuration;
    private final AtomicLong cleanupThreshold;
    private volatile int frameBufferDuration;
    private volatile boolean useSeekGhosting;

    // Additional services
    private final GarbageCollectionMonitor garbageCollectionMonitor;
    private final AudioPlayerLifecycleManager lifecycleManager;


    /**
     * Create a new instance
     */
    public DefaultAudioPlayerManager() {
        sourceManagers = new ArrayList<>();

        // Executors
        trackPlaybackExecutorService = new ThreadPoolExecutor(1, Integer.MAX_VALUE, 10, TimeUnit.SECONDS,
            new SynchronousQueue<>(), new DaemonThreadFactory("playback"));
        trackInfoExecutorService = ExecutorTools.createEagerlyScalingExecutor(1, DEFAULT_LOADER_POOL_SIZE,
            TimeUnit.SECONDS.toMillis(30), LOADER_QUEUE_CAPACITY, new DaemonThreadFactory("info-loader"));
        scheduledExecutorService = Executors.newScheduledThreadPool(1, new DaemonThreadFactory("manager"));
        orderedInfoExecutor = new OrderedExecutor(trackInfoExecutorService);

        // Configuration
        trackStuckThreshold = TimeUnit.MILLISECONDS.toNanos(10000);
        configuration = new AudioConfiguration();
        cleanupThreshold = new AtomicLong(DEFAULT_CLEANUP_THRESHOLD);
        frameBufferDuration = DEFAULT_FRAME_BUFFER_DURATION;
        useSeekGhosting = true;

        // Additional services
        garbageCollectionMonitor = new GarbageCollectionMonitor(scheduledExecutorService);
        lifecycleManager = new AudioPlayerLifecycleManager(scheduledExecutorService, cleanupThreshold);
        lifecycleManager.initialise();
    }

    @Override
    public void shutdown() {
        garbageCollectionMonitor.disable();
        lifecycleManager.shutdown();

        for (AudioSourceManager sourceManager : sourceManagers) {
            sourceManager.shutdown();
        }

        ExecutorTools.shutdownExecutor(trackPlaybackExecutorService, "track playback");
        ExecutorTools.shutdownExecutor(trackInfoExecutorService, "track info");
        ExecutorTools.shutdownExecutor(scheduledExecutorService, "scheduled operations");
    }

    @Override
    public void enableGcMonitoring() {
        garbageCollectionMonitor.enable();
    }

    @Override
    public void registerSourceManager(AudioSourceManager sourceManager) {
        sourceManagers.add(sourceManager);

        if (sourceManager instanceof HttpConfigurable) {
            Function<RequestConfig, RequestConfig> configurator = httpConfigurator;

            if (configurator != null) {
                ((HttpConfigurable) sourceManager).configureRequests(configurator);
            }

            Consumer<HttpClientBuilder> builderConfigurator = httpBuilderConfigurator;

            if (builderConfigurator != null) {
                ((HttpConfigurable) sourceManager).configureBuilder(builderConfigurator);
            }
        }
    }

    @Override
    public <T extends AudioSourceManager> T source(Class<T> klass) {
        for (AudioSourceManager sourceManager : sourceManagers) {
            if (klass.isAssignableFrom(sourceManager.getClass())) {
                return (T) sourceManager;
            }
        }

        return null;
    }

    @Override
    public List<AudioSourceManager> getSourceManagers() {
        return Collections.unmodifiableList(sourceManagers);
    }

    @Override
    public @Nullable AudioItem loadItemSync(AudioReference reference) {
        AudioItem item = checkSourcesForItem(reference);
        if (item == null) {
            log.debug("No matches for track with identifier {}.", reference.identifier);
        }

        return item;
    }

    @Override
    public void loadItemSync(final AudioReference reference, final AudioLoadResultHandler resultHandler) {
        boolean[] reported = new boolean[1];

        try {
            AudioItem item = loadItemSync(reference);
            submitItemToResultHandler(item, resultHandler, reported);
        } catch (Throwable throwable) {
            if (reported[0]) {
                log.warn("Load result handler for {} threw an exception", reference.identifier, throwable);
            } else {
                dispatchItemLoadFailure(reference.identifier, resultHandler, throwable);
            }

            ExceptionTools.rethrowErrors(throwable);
        }
    }

    @Override
    public Future<Void> loadItem(final AudioReference reference, final AudioLoadResultHandler resultHandler) {
        try {
            return trackInfoExecutorService.submit(() -> {
                loadItemSync(reference, resultHandler);
                return null;
            });
        } catch (RejectedExecutionException e) {
            return handleLoadRejected(reference.identifier, resultHandler, e);
        }
    }

    @Override
    public Future<Void> loadItemOrdered(Object orderingKey, final AudioReference reference, final AudioLoadResultHandler resultHandler) {
        try {
            return orderedInfoExecutor.submit(orderingKey, () -> {
                loadItemSync(reference, resultHandler);
                return null;
            });
        } catch (RejectedExecutionException e) {
            return handleLoadRejected(reference.identifier, resultHandler, e);
        }
    }

    private Future<Void> handleLoadRejected(String identifier, AudioLoadResultHandler resultHandler, RejectedExecutionException e) {
        FriendlyException exception = new FriendlyException("Cannot queue loading a track, queue is full.", SUSPICIOUS, e);
        ExceptionTools.log(log, exception, "queueing item " + identifier);

        resultHandler.loadFailed(exception);

        return ExecutorTools.COMPLETED_VOID;
    }

    private void dispatchItemLoadFailure(String identifier, AudioLoadResultHandler resultHandler, Throwable throwable) {
        FriendlyException exception = ExceptionTools.wrapUnfriendlyExceptions("Something went wrong when looking up the track", FAULT, throwable);
        ExceptionTools.log(log, exception, "loading item " + identifier);

        resultHandler.loadFailed(exception);
    }

    @Override
    public void encodeTrack(MessageOutput stream, AudioTrack track) throws IOException {
        DataOutput output = stream.startMessage();
        output.write(TRACK_INFO_VERSION);

        AudioTrackInfo trackInfo = track.getInfo();
        output.writeUTF(trackInfo.title);
        output.writeUTF(trackInfo.author);
        output.writeLong(trackInfo.length);
        output.writeUTF(trackInfo.identifier);
        output.writeBoolean(trackInfo.isStream);
        DataFormatTools.writeNullableText(output, trackInfo.uri);
        DataFormatTools.writeNullableText(output, trackInfo.artworkUrl);
        DataFormatTools.writeNullableText(output, trackInfo.isrc);

        encodeTrackDetails(track, output);
        output.writeLong(track.getPosition());

        stream.commitMessage(TRACK_INFO_VERSIONED);
    }

    @Override
    public DecodedTrackHolder decodeTrack(MessageInput stream) throws IOException {
        DataInput input = stream.nextMessage();
        if (input == null) {
            return null;
        }

        int version = (stream.getMessageFlags() & TRACK_INFO_VERSIONED) != 0 ? (input.readByte() & 0xFF) : 1;

        AudioTrackInfo trackInfo = new AudioTrackInfo(
            input.readUTF(),
            input.readUTF(),
            input.readLong(),
            input.readUTF(),
            input.readBoolean(),
            version >= 2 ? DataFormatTools.readNullableText(input) : null,
            version >= 3 ? DataFormatTools.readNullableText(input) : null,
            version >= 3 ? DataFormatTools.readNullableText(input) : null
        );
        AudioTrack track = decodeTrackDetails(trackInfo, input);
        long position = input.readLong();

        if (track != null) {
            track.setPosition(position);
        }

        stream.skipRemainingBytes();

        return new DecodedTrackHolder(track);
    }

    /**
     * Encodes an audio track to a byte array. Does not include AudioTrackInfo in the buffer.
     *
     * @param track The track to encode
     * @return The bytes of the encoded data
     */
    public byte[] encodeTrackDetails(AudioTrack track) {
        try {
            ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
            DataOutput output = new DataOutputStream(byteOutput);

            encodeTrackDetails(track, output);

            return byteOutput.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void encodeTrackDetails(AudioTrack track, DataOutput output) throws IOException {
        AudioSourceManager sourceManager = track.getSourceManager();
        output.writeUTF(sourceManager.getSourceName());
        sourceManager.encodeTrack(track, output);
    }

    /**
     * Decodes an audio track from a byte array.
     *
     * @param trackInfo Track info for the track to decode
     * @param buffer    Byte array containing the encoded track
     * @return Decoded audio track
     */
    public AudioTrack decodeTrackDetails(AudioTrackInfo trackInfo, byte[] buffer) {
        try {
            DataInput input = new DataInputStream(new ByteArrayInputStream(buffer));
            return decodeTrackDetails(trackInfo, input);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private AudioTrack decodeTrackDetails(AudioTrackInfo trackInfo, DataInput input) throws IOException {
        String sourceName = input.readUTF();

        for (AudioSourceManager sourceManager : sourceManagers) {
            if (sourceName.equals(sourceManager.getSourceName())) {
                return sourceManager.decodeTrack(trackInfo, input);
            }
        }

        return null;
    }

    /**
     * Executes an audio track with the given player and volume.
     *
     * @param listener      A listener for track state events
     * @param track         The audio track to execute
     * @param configuration The audio configuration to use for executing
     * @param playerOptions Options of the audio player
     */
    public void executeTrack(TrackStateListener listener, InternalAudioTrack track, AudioConfiguration configuration,
                             AudioPlayerOptions playerOptions) {

        final AudioTrackExecutor executor = createExecutorForTrack(track, configuration, playerOptions);
        track.assignExecutor(executor, true);

        trackPlaybackExecutorService.execute(() -> executor.execute(listener));
    }

    private AudioTrackExecutor createExecutorForTrack(InternalAudioTrack track, AudioConfiguration configuration,
                                                      AudioPlayerOptions playerOptions) {

        AudioTrackExecutor customExecutor = track.createLocalExecutor(this);
        if (customExecutor != null) {
            return customExecutor;
        } else {
            int bufferDuration = Optional.ofNullable(playerOptions.frameBufferDuration.get()).orElse(frameBufferDuration);
            return new LocalAudioTrackExecutor(track, configuration, playerOptions, useSeekGhosting, bufferDuration);

        }
    }

    @Override
    public AudioConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public boolean isUsingSeekGhosting() {
        return useSeekGhosting;
    }

    @Override
    public void setUseSeekGhosting(boolean useSeekGhosting) {
        this.useSeekGhosting = useSeekGhosting;
    }

    @Override
    public int getFrameBufferDuration() {
        return frameBufferDuration;
    }

    @Override
    public void setFrameBufferDuration(int frameBufferDuration) {
        this.frameBufferDuration = Math.max(200, frameBufferDuration);
    }

    @Override
    public void setTrackStuckThreshold(long trackStuckThreshold) {
        this.trackStuckThreshold = TimeUnit.MILLISECONDS.toNanos(trackStuckThreshold);
    }

    public long getTrackStuckThresholdNanos() {
        return trackStuckThreshold;
    }

    @Override
    public void setPlayerCleanupThreshold(long cleanupThreshold) {
        this.cleanupThreshold.set(cleanupThreshold);
    }

    @Override
    public void setItemLoaderThreadPoolSize(int poolSize) {
        trackInfoExecutorService.setMaximumPoolSize(poolSize);
    }

    private void submitItemToResultHandler(AudioItem item, AudioLoadResultHandler handler, boolean[] reported) {
        if (item == null) {
            reported[0] = true;
            handler.noMatches();
        } else if (item instanceof AudioTrack) {
            reported[0] = true;
            handler.trackLoaded((AudioTrack) item);
        } else if (item instanceof AudioPlaylist) {
            reported[0] = true;
            handler.playlistLoaded((AudioPlaylist) item);
        } else {
            log.warn("Cannot submit unknown item to result handler: {}", item);
        }
    }

    /**
     * Attempts to load the provided {@link AudioReference} using the sources registered with this {@link AudioPlayerManager}.
     * Unlike {@link #checkSourcesForItemOnce} this method attempts to follow any returned redirects.
     */
    @Nullable
    private AudioItem checkSourcesForItem(AudioReference reference) {
        AudioReference currentReference = reference;

        for (int redirects = 0; redirects < MAXIMUM_LOAD_REDIRECTS && currentReference.identifier != null; redirects++) {
            AudioItem item = checkSourcesForItemOnce(currentReference);
            if (item instanceof AudioReference) {
                currentReference = (AudioReference) item;
                continue;
            }

            return item;
        }

        return null;
    }

    @Nullable
    private AudioItem checkSourcesForItemOnce(AudioReference reference) {
        for (AudioSourceManager sourceManager : sourceManagers) {
            if (reference.containerDescriptor != null && !(sourceManager instanceof ProbingAudioSourceManager)) {
                continue;
            }

            AudioItem item = sourceManager.loadItem(this, reference);
            if (item != null) {
                if (item instanceof AudioTrack) {
                    log.debug("Loaded a track with identifier {} using {}.", reference.identifier, sourceManager.getClass().getSimpleName());
                } else if (item instanceof AudioPlaylist) {
                    log.debug("Loaded a playlist with identifier {} using {}.", reference.identifier, sourceManager.getClass().getSimpleName());
                }

                return item;
            }
        }

        return null;
    }

    public ExecutorService getExecutor() {
        return trackPlaybackExecutorService;
    }

    @Override
    public AudioPlayer createPlayer() {
        AudioPlayer player = constructPlayer();
        player.addListener(lifecycleManager);

        return player;
    }

    protected AudioPlayer constructPlayer() {
        return new DefaultAudioPlayer(this);
    }

    @Override
    public void setHttpRequestConfigurator(Function<RequestConfig, RequestConfig> configurator) {
        this.httpConfigurator = configurator;

        if (configurator != null) {
            for (AudioSourceManager sourceManager : sourceManagers) {
                if (sourceManager instanceof HttpConfigurable) {
                    ((HttpConfigurable) sourceManager).configureRequests(configurator);
                }
            }
        }
    }

    @Override
    public void setHttpBuilderConfigurator(Consumer<HttpClientBuilder> configurator) {
        this.httpBuilderConfigurator = configurator;

        if (configurator != null) {
            for (AudioSourceManager sourceManager : sourceManagers) {
                if (sourceManager instanceof HttpConfigurable) {
                    ((HttpConfigurable) sourceManager).configureBuilder(configurator);
                }
            }
        }
    }
}
