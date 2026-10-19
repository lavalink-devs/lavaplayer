package com.sedmelluq.discord.lavaplayer.source.soundcloud;

import com.sedmelluq.discord.lavaplayer.container.playlists.ExtendedM3uParser;
import com.sedmelluq.discord.lavaplayer.container.playlists.HlsStreamSegment;
import com.sedmelluq.discord.lavaplayer.container.playlists.HlsStreamSegmentParser;
import com.sedmelluq.discord.lavaplayer.tools.http.HttpStreamTools;
import com.sedmelluq.discord.lavaplayer.tools.io.ChainedInputStream;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.tools.io.NonSeekableInputStream;
import com.sedmelluq.discord.lavaplayer.tools.io.SeekableInputStream;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.DelegatedAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class SoundCloudM3uAudioTrack extends DelegatedAudioTrack {
    private static final Logger log = LoggerFactory.getLogger(SoundCloudM3uAudioTrack.class);

    private static final long SEGMENT_UPDATE_INTERVAL = TimeUnit.MINUTES.toMillis(10);

    private final HttpInterface httpInterface;
    private final SoundCloudM3uInfo m3uInfo;

    public SoundCloudM3uAudioTrack(AudioTrackInfo trackInfo, HttpInterface httpInterface, SoundCloudM3uInfo m3uInfo) {
        super(trackInfo);
        this.httpInterface = httpInterface;
        this.m3uInfo = m3uInfo;
    }

    @Override
    public void process(LocalAudioTrackExecutor localExecutor) throws Exception {
        try (SegmentTracker segmentTracker = createSegmentTracker()) {
            segmentTracker.decoder.prepareStream(true);

            localExecutor.executeProcessingLoop(() -> segmentTracker.decoder.playStream(
                localExecutor.getProcessingContext(),
                segmentTracker.streamStartPosition,
                segmentTracker.desiredPosition
            ), segmentTracker::seekToTimecode, true);
        }
    }

    private SegmentList loadSegments() throws IOException {
        String playbackUrl = SoundCloudHelper.loadPlaybackUrl(httpInterface, m3uInfo.lookupUrl);
        String[] lines = HttpClientTools.fetchResponseLines(httpInterface, new HttpGet(playbackUrl), "stream segments list");
        return new SegmentList(findInitSegmentUrl(lines), HlsStreamSegmentParser.parseFromLines(lines));
    }

    private static String findInitSegmentUrl(String[] lines) {
        for (String lineText : lines) {
            ExtendedM3uParser.Line line = ExtendedM3uParser.parseLine(lineText);

            if (line.isDirective() && "EXT-X-MAP".equals(line.directiveName)) {
                return line.directiveArguments.get("URI");
            }
        }

        return null;
    }

    private SegmentTracker createSegmentTracker() throws IOException {
        SegmentList initialSegments = loadSegments();
        SegmentTracker tracker = new SegmentTracker(initialSegments);
        tracker.setupDecoder(m3uInfo.decoderFactory);
        return tracker;
    }

    private static class SegmentList {
        private final String initSegmentUrl;
        private final List<HlsStreamSegment> segments;

        private SegmentList(String initSegmentUrl, List<HlsStreamSegment> segments) {
            this.initSegmentUrl = initSegmentUrl;
            this.segments = segments;
        }
    }

    private class SegmentTracker implements AutoCloseable {
        private final List<HlsStreamSegment> segments;
        private String initSegmentUrl;
        private boolean initSegmentPending;
        private long desiredPosition = 0;
        private long streamStartPosition = 0;
        private long lastUpdate;
        private SoundCloudSegmentDecoder decoder;
        private int segmentIndex = 0;

        private SegmentTracker(SegmentList segmentList) {
            this.segments = segmentList.segments;
            this.initSegmentUrl = segmentList.initSegmentUrl;
            this.lastUpdate = System.currentTimeMillis();
        }

        private void setupDecoder(SoundCloudSegmentDecoder.Factory factory) {
            decoder = factory.create(this::createChainedStream);
        }

        private SeekableInputStream createChainedStream() {
            // Fragmented MP4 segments are only decodable after the initialization segment.
            initSegmentPending = initSegmentUrl != null;
            return new NonSeekableInputStream(new ChainedInputStream(this::getNextStream));
        }

        private void seekToTimecode(long timecode) throws IOException {
            long segmentTimecode = 0;

            for (int i = 0; i < segments.size(); i++) {
                Long duration = segments.get(i).duration;

                if (duration == null) {
                    break;
                }

                long nextTimecode = segmentTimecode + duration;

                if (timecode >= segmentTimecode && timecode < nextTimecode) {
                    seekToSegment(i, timecode, segmentTimecode);
                    return;
                }

                segmentTimecode = nextTimecode;
            }

            seekToEnd();
        }

        private void seekToSegment(int index, long requestedTimecode, long segmentTimecode) throws IOException {
            decoder.resetStream();

            segmentIndex = index;
            desiredPosition = requestedTimecode;
            streamStartPosition = segmentTimecode;

            decoder.prepareStream(streamStartPosition == 0);
        }

        private void seekToEnd() throws IOException {
            decoder.resetStream();

            segmentIndex = segments.size();
        }

        private InputStream getNextStream() {
            if (initSegmentPending) {
                initSegmentPending = false;
                return HttpStreamTools.streamContent(httpInterface, new HttpGet(initSegmentUrl));
            }

            HlsStreamSegment segment = getNextSegment();

            if (segment == null) {
                return null;
            }

            return HttpStreamTools.streamContent(httpInterface, new HttpGet(segment.url));
        }

        private void updateSegmentList() {
            try {
                SegmentList newSegmentList = loadSegments();
                List<HlsStreamSegment> newSegments = newSegmentList.segments;

                if (newSegments.size() != segments.size()) {
                    log.error("For {}, received different number of segments on update, skipping.", trackInfo.identifier);
                    return;
                }

                for (int i = 0; i < segments.size(); i++) {
                    if (!Objects.equals(newSegments.get(i).duration, segments.get(i).duration)) {
                        log.error("For {}, segment {} has different length than previously on update.", trackInfo.identifier, i);
                        return;
                    }
                }

                for (int i = 0; i < segments.size(); i++) {
                    segments.set(i, newSegments.get(i));
                }

                initSegmentUrl = newSegmentList.initSegmentUrl;
            } catch (Exception e) {
                log.error("For {}, failed to update segment list, skipping.", trackInfo.identifier, e);
            }
        }

        private void checkSegmentListUpdate() {
            long now = System.currentTimeMillis();
            long delta = now - lastUpdate;

            if (delta > SEGMENT_UPDATE_INTERVAL) {
                log.debug("For {}, {}ms has passed since last segment update, updating", trackInfo.identifier, delta);

                updateSegmentList();
                lastUpdate = now;
            }
        }

        private HlsStreamSegment getNextSegment() {
            int current = segmentIndex++;

            if (current < segments.size()) {
                checkSegmentListUpdate();
                return segments.get(current);
            } else {
                return null;
            }
        }

        @Override
        public void close() throws Exception {
            decoder.resetStream();
        }
    }
}
