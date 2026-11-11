package com.sedmelluq.discord.lavaplayer.source.local;

import com.sedmelluq.discord.lavaplayer.container.*;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.ProbingAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;

/**
 * Audio source manager that implements finding audio files from the local file system.
 */
public class LocalAudioSourceManager extends ProbingAudioSourceManager {
    public LocalAudioSourceManager() {
        this(MediaContainerRegistry.DEFAULT_REGISTRY);
    }

    public LocalAudioSourceManager(MediaContainerRegistry containerRegistry) {
        super(containerRegistry);
    }

    @Override
    public String getSourceName() {
        return "local";
    }

    @Override
    public AudioItem loadItem(AudioPlayerManager manager, AudioReference reference) {
        File file = new File(reference.identifier);

        if (file.exists() && file.isFile() && file.canRead()) {
            return handleLoadResult(detectContainerForFile(reference, file));
        } else {
            return null;
        }
    }

    @Override
    protected AudioTrack createTrack(AudioTrackInfo trackInfo, MediaContainerDescriptor containerTrackFactory) {
        return new LocalAudioTrack(trackInfo, containerTrackFactory, this);
    }

    private MediaContainerDetectionResult detectContainerForFile(AudioReference reference, File file) {
        try (LocalSeekableInputStream inputStream = new LocalSeekableInputStream(file)) {
            int lastDotIndex = file.getName().lastIndexOf('.');
            String fileExtension = lastDotIndex >= 0 ? file.getName().substring(lastDotIndex + 1) : null;

            return new MediaContainerDetection(containerRegistry, referenceWithFallbackTitle(reference, file.getName()),
                inputStream, MediaContainerHints.from(null, fileExtension)).detectContainer();
        } catch (IOException e) {
            throw new FriendlyException("Failed to open file for reading.", SUSPICIOUS, e);
        }
    }

    /**
     * Wraps the reference with the file name as a fallback title when no title is known, so that embedded metadata can
     * still take precedence, as it is applied to the builder afterwards.
     */
    private static AudioReference referenceWithFallbackTitle(AudioReference reference, String fileName) {
        if (reference.title != null) {
            return reference;
        }

        int lastDotIndex = fileName.lastIndexOf('.');
        String title = lastDotIndex > 0 ? fileName.substring(0, lastDotIndex) : fileName;

        return new AudioReference(reference.identifier, title, reference.containerDescriptor);
    }

    @Override
    public boolean isTrackEncodable(AudioTrack track) {
        return true;
    }

    @Override
    public void encodeTrack(AudioTrack track, DataOutput output) throws IOException {
        encodeTrackFactory(((LocalAudioTrack) track).getContainerTrackFactory(), output);
    }

    @Override
    public AudioTrack decodeTrack(AudioTrackInfo trackInfo, DataInput input) throws IOException {
        MediaContainerDescriptor containerTrackFactory = decodeTrackFactory(input);

        if (containerTrackFactory != null) {
            return new LocalAudioTrack(trackInfo, containerTrackFactory, this);
        }

        return null;
    }

    @Override
    public void shutdown() {
        // Nothing to shut down
    }
}
