package com.sedmelluq.discord.lavaplayer.container.mpeg;

import com.sedmelluq.discord.lavaplayer.container.common.AacPacketRouter;
import com.sedmelluq.discord.lavaplayer.natives.aac.AacDecoder;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioProcessingContext;
import net.sourceforge.jaad.aac.Decoder;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ReadableByteChannel;

/**
 * Handles processing MP4 AAC frames. Passes the decoded frames to the specified frame consumer. Currently only AAC LC
 * format is supported, although the underlying decoder can handler other types as well.
 */
public class MpegAacTrackConsumer implements MpegTrackConsumer {
    private static final Logger log = LoggerFactory.getLogger(MpegAacTrackConsumer.class);

    private final MpegTrackInfo track;
    private final AacPacketRouter packetRouter;

    private ByteBuffer inputBuffer;
    private boolean configured;

    /**
     * @param context Configuration and output information for processing
     * @param track   The MP4 audio track descriptor
     */
    public MpegAacTrackConsumer(AudioProcessingContext context, MpegTrackInfo track) {
        this.track = track;
        this.packetRouter = new AacPacketRouter(context);
    }

    @Override
    public void initialise() {
        log.debug("Initialising AAC track with expected frequency {} and channel count {}.",
            track.sampleRate, track.channelCount);
    }

    @Override
    public MpegTrackInfo getTrack() {
        return track;
    }

    @Override
    public void seekPerformed(long requestedTimecode, long providedTimecode) {
        packetRouter.seekPerformed(requestedTimecode, providedTimecode);
    }

    @Override
    public void flush() throws InterruptedException {
        packetRouter.flush();
    }

    @Override
    public void consume(ReadableByteChannel channel, int length) throws InterruptedException {
        if (packetRouter.nativeDecoder == null) {
            packetRouter.nativeDecoder = new AacDecoder();
            configured = configureDecoder(packetRouter.nativeDecoder);
        }

        if (configured) {
            if (inputBuffer == null) {
                inputBuffer = ByteBuffer.allocateDirect(4096);
            }

            processInput(channel, length);
        } else {
            if (packetRouter.embeddedDecoder == null) {
                if (track.decoderConfig != null) {
                    packetRouter.embeddedDecoder = Decoder.create(track.decoderConfig);
                } else {
                    packetRouter.embeddedDecoder = Decoder.create(AacDecoder.AAC_LC, track.sampleRate, track.channelCount);
                }
                inputBuffer = ByteBuffer.allocate(4096);
            }

            processInput(channel, length);
        }
    }

    private void processInput(ReadableByteChannel channel, int length) throws InterruptedException {
        int remaining = length;

        while (remaining > 0) {
            int chunk = Math.min(remaining, inputBuffer.capacity());

            inputBuffer.clear();
            inputBuffer.limit(chunk);

            try {
                IOUtils.readFully(channel, inputBuffer);
            } catch (ClosedByInterruptException e) {
                log.trace("Interrupt received while reading channel", e);

                Thread.currentThread().interrupt();
                throw new InterruptedException();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            inputBuffer.flip();
            packetRouter.processInput(inputBuffer);

            remaining -= chunk;
        }
    }

    @Override
    public void close() {
        packetRouter.close();
    }

    private boolean configureDecoder(AacDecoder decoder) {
        if (track.decoderConfig != null) {
            return (decoder.configure(track.decoderConfig) == 0);
        } else {
            return (decoder.configure(AacDecoder.AAC_LC, track.sampleRate, track.channelCount) == 0);
        }
    }
}
