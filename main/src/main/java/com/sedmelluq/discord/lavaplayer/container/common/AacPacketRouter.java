package com.sedmelluq.discord.lavaplayer.container.common;

import com.sedmelluq.discord.lavaplayer.filter.AudioPipeline;
import com.sedmelluq.discord.lavaplayer.filter.AudioPipelineFactory;
import com.sedmelluq.discord.lavaplayer.filter.PcmFormat;
import com.sedmelluq.discord.lavaplayer.natives.aac.AacDecoder;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioProcessingContext;
import net.sourceforge.jaad.aac.Decoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

public class AacPacketRouter {
    private static final Logger log = LoggerFactory.getLogger(AacPacketRouter.class);

    private final AudioProcessingContext context;

    private Long initialRequestedTimecode;
    private Long initialProvidedTimecode;
    private AudioPipeline downstream;
    private ShortBuffer outputBuffer;

    public AacDecoder nativeDecoder;
    public Decoder embeddedDecoder;

    public AacPacketRouter(AudioProcessingContext context) {
        this.context = context;
    }

    public void processInput(ByteBuffer inputBuffer) throws InterruptedException {
        if (embeddedDecoder == null) {
            nativeDecoder.fill(inputBuffer);

            if (downstream == null) {
                log.debug("Using native decoder");
                AacDecoder.StreamInfo streamInfo = nativeDecoder.resolveStreamInfo();

                if (streamInfo != null) {
                    downstream = AudioPipelineFactory.create(context, new PcmFormat(streamInfo.channels, streamInfo.sampleRate));
                    outputBuffer = ByteBuffer.allocateDirect(2 * streamInfo.frameSize * streamInfo.channels)
                        .order(ByteOrder.nativeOrder()).asShortBuffer();

                    if (initialRequestedTimecode != null) {
                        downstream.seekPerformed(initialRequestedTimecode, initialProvidedTimecode);
                    }
                }
            }

            if (downstream != null) {
                while (nativeDecoder.decode(outputBuffer, false)) {
                    downstream.process(outputBuffer);
                    outputBuffer.clear();
                }
            }
        } else {
            if (downstream == null) {
                log.debug("Using embedded decoder");
                downstream = AudioPipelineFactory.create(context, new PcmFormat(
                    embeddedDecoder.getAudioFormat().getChannels(),
                    (int) embeddedDecoder.getAudioFormat().getSampleRate()
                ));

                if (initialRequestedTimecode != null) {
                    downstream.seekPerformed(initialRequestedTimecode, initialProvidedTimecode);
                }
            }

            if (downstream != null) {
                downstream.process(embeddedDecoder.decodeFrame(inputBuffer.array()));
            }
        }
    }

    public void seekPerformed(long requestedTimecode, long providedTimecode) {
        if (downstream != null) {
            downstream.seekPerformed(requestedTimecode, providedTimecode);
        } else {
            this.initialRequestedTimecode = requestedTimecode;
            this.initialProvidedTimecode = providedTimecode;
        }

        if (nativeDecoder != null) {
            nativeDecoder.close();
            nativeDecoder = null;
        } else if (embeddedDecoder != null) {
            embeddedDecoder = null;
        }
    }

    public void flush() throws InterruptedException {
        if (downstream != null) {
            while (nativeDecoder.decode(outputBuffer, true)) {
                downstream.process(outputBuffer);
                outputBuffer.clear();
            }
        }
    }

    public void close() {
        try {
            if (downstream != null) {
                downstream.close();
            }
        } finally {
            if (nativeDecoder != null) {
                nativeDecoder.close();
            } else if (embeddedDecoder != null) {
                embeddedDecoder = null;
            }
        }
    }
}
