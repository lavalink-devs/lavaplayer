package com.sedmelluq.discord.lavaplayer.container.matroska;

import com.sedmelluq.discord.lavaplayer.container.common.AacPacketRouter;
import com.sedmelluq.discord.lavaplayer.container.matroska.format.MatroskaFileTrack;
import com.sedmelluq.discord.lavaplayer.container.mpeg.MpegAacTrackConsumer;
import com.sedmelluq.discord.lavaplayer.natives.aac.AacDecoder;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioProcessingContext;
import net.sourceforge.jaad.aac.Decoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

/**
 * Consumes AAC track data from a matroska file.
 */
public class MatroskaAacTrackConsumer implements MatroskaTrackConsumer {
    private static final Logger log = LoggerFactory.getLogger(MpegAacTrackConsumer.class);

    private final MatroskaFileTrack track;
    private final AacPacketRouter packetRouter;

    private ByteBuffer inputBuffer;
    private boolean configured;

    /**
     * @param context Configuration and output information for processing
     * @param track   The MP4 audio track descriptor
     */
    public MatroskaAacTrackConsumer(AudioProcessingContext context, MatroskaFileTrack track) {
        this.track = track;
        this.packetRouter = new AacPacketRouter(context);
    }

    @Override
    public void initialise() {
        log.debug("Initialising AAC track with expected frequency {} and channel count {}.",
            track.audio.samplingFrequency, track.audio.channels);
    }

    @Override
    public MatroskaFileTrack getTrack() {
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
    public void consume(ByteBuffer data) throws InterruptedException {
        if (packetRouter.nativeDecoder == null) {
            packetRouter.nativeDecoder = new AacDecoder();
            configured = configureDecoder(packetRouter.nativeDecoder);
        }

        if (configured) {
            if (inputBuffer == null) {
                inputBuffer = ByteBuffer.allocateDirect(4096);
            }

            processInput(data);
        } else {
            if (packetRouter.embeddedDecoder == null) {
                packetRouter.embeddedDecoder = Decoder.create(track.codecPrivate);
                inputBuffer = ByteBuffer.allocate(4096);
            }

            processInput(data);
        }
    }

    private void processInput(ByteBuffer data) throws InterruptedException {
        while (data.hasRemaining()) {
            int chunk = Math.min(data.remaining(), inputBuffer.capacity());
            ByteBuffer chunkBuffer = data.duplicate();
            chunkBuffer.limit(chunkBuffer.position() + chunk);

            inputBuffer.clear();
            inputBuffer.put(chunkBuffer);
            inputBuffer.flip();

            packetRouter.processInput(inputBuffer);

            data.position(chunkBuffer.position());
        }
    }

    @Override
    public void close() {
        packetRouter.close();
    }

    private boolean configureDecoder(AacDecoder decoder) {
        return (decoder.configure(track.codecPrivate) == 0);
    }
}
