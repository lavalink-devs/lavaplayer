package com.sedmelluq.discord.lavaplayer.source.soundcloud;

import com.sedmelluq.discord.lavaplayer.container.mpeg.MpegAacTrackConsumer;
import com.sedmelluq.discord.lavaplayer.container.mpeg.MpegFileLoader;
import com.sedmelluq.discord.lavaplayer.container.mpeg.MpegTrackInfo;
import com.sedmelluq.discord.lavaplayer.container.mpeg.reader.MpegFileTrackProvider;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.io.SeekableInputStream;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioProcessingContext;

import java.io.IOException;
import java.util.function.Supplier;

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;

/**
 * Decoder for HLS streams with AAC audio in fragmented MP4 segments. Expects the provided stream to start with the
 * initialization segment (EXT-X-MAP) followed by the media segments.
 */
public class SoundCloudAacSegmentDecoder implements SoundCloudSegmentDecoder {
    private final Supplier<SeekableInputStream> nextStreamProvider;

    public SoundCloudAacSegmentDecoder(Supplier<SeekableInputStream> nextStreamProvider) {
        this.nextStreamProvider = nextStreamProvider;
    }

    @Override
    public void prepareStream(boolean beginning) {
        // Nothing to do.
    }

    @Override
    public void resetStream() {
        // Nothing to do.
    }

    @Override
    public void playStream(
        AudioProcessingContext context,
        long startPosition,
        long desiredPosition
    ) throws InterruptedException, IOException {
        try (SeekableInputStream stream = nextStreamProvider.get()) {
            MpegFileLoader file = new MpegFileLoader(stream);
            file.parseHeaders();

            MpegTrackInfo track = findAacTrack(file);
            MpegAacTrackConsumer consumer = new MpegAacTrackConsumer(context, track);

            try {
                consumer.initialise();

                MpegFileTrackProvider fileReader = file.loadReader(consumer);

                if (fileReader == null) {
                    throw new FriendlyException("Unknown MP4 format in SoundCloud stream.", SUSPICIOUS, null);
                }

                // Persist the position to seek to
                consumer.seekPerformed(desiredPosition, startPosition);

                fileReader.provideFrames();
            } finally {
                consumer.close();
            }
        }
    }

    @Override
    public void close() {
        // Nothing to do.
    }

    private static MpegTrackInfo findAacTrack(MpegFileLoader file) {
        for (MpegTrackInfo track : file.getTrackList()) {
            if ("soun".equals(track.handler) && "mp4a".equals(track.codecName)) {
                return track;
            }
        }

        throw new FriendlyException("No AAC audio track found in SoundCloud stream.", SUSPICIOUS, null);
    }
}
