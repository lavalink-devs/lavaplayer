package com.sedmelluq.discord.lavaplayer.natives.mp3;

import com.sedmelluq.lava.common.natives.NativeResourceHolder;

import java.util.Arrays;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

/**
 * A wrapper around the native methods of OpusDecoderLibrary.
 */
public class Mp3Decoder extends NativeResourceHolder {
    public static final long MPEG1_SAMPLES_PER_FRAME = 1152;
    public static final long MPEG2_SAMPLES_PER_FRAME = 576;
    public static final int HEADER_SIZE = 4;

    private static final int ERROR_NEED_MORE = -10;
    private static final int ERROR_NEW_FORMAT = -11;

    private final Mp3DecoderLibrary library;
    private final long instance;

    /**
     * Create a new instance of mp3 decoder
     */
    public Mp3Decoder() {
        library = Mp3DecoderLibrary.getInstance();
        instance = library.create();

        if (instance == 0) {
            throw new IllegalStateException("Failed to create a decoder instance");
        }
    }

    /**
     * Encode the input buffer to output.
     *
     * @param directInput  Input byte buffer
     * @param directOutput Output sample buffer
     * @return Number of samples written to the output
     */
    public int decode(ByteBuffer directInput, ShortBuffer directOutput) {
        checkNotReleased();

        if (!directInput.isDirect() || !directOutput.isDirect()) {
            throw new IllegalArgumentException("Arguments must be direct buffers.");
        }

        int result = library.decode(instance, directInput, directInput.remaining(), directOutput, directOutput.remaining() * 2);

        while (result == ERROR_NEW_FORMAT) {
            result = library.decode(instance, directInput, 0, directOutput, directOutput.remaining() * 2);
        }

        if (result == ERROR_NEED_MORE) {
            result = 0;
        } else if (result < 0) {
            throw new IllegalStateException("Decoding failed with error " + result);
        }

        directOutput.position(result / 2);
        directOutput.flip();

        return result / 2;
    }

    @Override
    protected void freeResources() {
        library.destroy(instance);
    }

    private static int getFrameBitRate(byte[] buffer, int offset) {
        return MpegVersion.getVersion(buffer, offset).getBitRate(buffer, offset);
    }

    /**
     * Get the sample rate for the current frame
     *
     * @param buffer Buffer which contains the frame header
     * @param offset Offset to the frame header
     * @return Sample rate
     */
    public static int getFrameSampleRate(byte[] buffer, int offset) {
        return MpegVersion.getVersion(buffer, offset).getSampleRate(buffer, offset);
    }

    /**
     * Get the number of channels in the current frame
     *
     * @param buffer Buffer which contains the frame header
     * @param offset Offset to the frame header
     * @return Number of channels
     */
    public static int getFrameChannelCount(byte[] buffer, int offset) {
        return (buffer[offset + 3] & 0xC0) == 0xC0 ? 1 : 2;
    }

    public static boolean hasFrameSync(byte[] buffer, int offset) {
        // must start with 11 high bits
        return (buffer[offset] & 0xFF) == 0xFF && (buffer[offset + 1] & 0xE0) == 0xE0;
    }

    public static boolean isUnsupportedVersion(byte[] buffer, int offset) {
        return (buffer[offset + 1] & 0x18) >> 3 == 0x01;
    }

    public static boolean isValidFrame(byte[] buffer, int offset) {
        int second = buffer[offset + 1] & 0xFF;
        int third = buffer[offset + 2] & 0xFF;
        return (second & 0x06) == 0x02 // Is Layer III
            && (third & 0xF0) != 0x00 // Has defined bitrate
            && (third & 0xF0) != 0xF0 // Valid bitrate
            && (third & 0x0C) != 0x0C; // Valid sampling rate
    }

    /**
     * Get the frame size of the specified 4 bytes
     *
     * @param buffer Buffer which contains the frame header
     * @param offset Offset to the frame header
     * @return Frame size, or zero if not a valid frame header
     */
    public static int getFrameSize(byte[] buffer, int offset) {
        return MpegVersion.getVersion(buffer, offset).getFrameSize(buffer, offset);
    }

    /**
     * Get the average frame size based on this frame
     *
     * @param buffer Buffer which contains the frame header
     * @param offset Offset to the frame header
     * @return Average frame size, assuming CBR
     */
    public static double getAverageFrameSize(byte[] buffer, int offset) {
        return MpegVersion.getVersion(buffer, offset).getAverageFrameSize(buffer, offset);
    }

    /**
     * @param buffer Buffer which contains the frame header
     * @param offset Offset to the frame header
     * @return Number of samples per frame.
     */
    public static long getSamplesPerFrame(byte[] buffer, int offset) {
        return MpegVersion.getVersion(buffer, offset).getSamplesPerFrame();
    }

    public static int getMaximumFrameSize() {
        return MpegVersion.MAX_FRAME_SIZE;
    }

    private static final int[] SAMPLE_RATE_BASE = { 11025, 12000, 8000 };

    public static enum MpegVersion {

        MPEG_1(4, 1152, new int[] { 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320 }),
        MPEG_2(2, 576, new int[] { 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160 }),
        MPEG_2_5(1, MPEG_2.samplesPerFrame, MPEG_2.bitrateIndex);

        public static final int MAX_FRAME_SIZE = getMaxFrameSize();

        public static MpegVersion getVersion(byte[] buffer, int offset) {
            // 0 - MPEG 2.5
            // 1 - reserved (unsupported)
            // 2 - MPEG 2
            // 3 - MPEG 1
            int index = (buffer[offset + 1] & 0x18) >> 3;
            switch (index) {
                case 0:
                    return MPEG_2_5;
                case 2:
                    return MPEG_2;
                case 3:
                    return MPEG_1;
                default:
                    throw new IllegalArgumentException("Invalid version");
            }
        }

        private static int getMaxFrameSize() {
            int bitRate = MPEG_1.bitrateIndex[MPEG_1.bitrateIndex.length - 1] * 1000;
            int sampleRate = MPEG_1.samplerateIndex[2];
            return MPEG_1.calculateFrameSize(bitRate, sampleRate, true);
        }

        private final int samplesPerFrame;
        private final int frameLengthMultiplier;
        private final int[] bitrateIndex;
        private final int[] samplerateIndex;

        MpegVersion(int samplerateMultiplier, int samplesPerFrame, int[] bitrateIndex) {
            this.samplesPerFrame = samplesPerFrame;
            this.frameLengthMultiplier = samplesPerFrame / 8;
            this.bitrateIndex = bitrateIndex;
            this.samplerateIndex = Arrays.stream(SAMPLE_RATE_BASE).map(r -> r * samplerateMultiplier).toArray();
        }

        public int getSamplesPerFrame() {
            return this.samplesPerFrame;
        }

        public int getFrameLengthMultiplier() {
            return this.frameLengthMultiplier;
        }

        public int getBitRate(byte[] buffer, int offset) {
            int index = (buffer[offset + 2] & 0xF0) >> 4;
            if (index == 0 || index == 15)
                throw new IllegalArgumentException("Invalid bitrate");

            return this.bitrateIndex[index - 1] * 1000;
        }

        public int getSampleRate(byte[] buffer, int offset) {
            int index = (buffer[offset + 2] & 0x0C) >> 2;
            if (index == 3)
                throw new IllegalArgumentException("Invalid samplerate");

            return this.samplerateIndex[index];
        }

        public int getFrameSize(byte[] buffer, int offset) {
            return calculateFrameSize(getBitRate(buffer, offset), getSampleRate(buffer, offset),
                                      hasPadding(buffer, offset));
        }

        public double getAverageFrameSize(byte[] buffer, int offset) {
            return (double) getFrameLengthMultiplier() * getBitRate(buffer, offset) / getSampleRate(buffer, offset);
        }

        private int calculateFrameSize(int bitRate, int sampleRate, boolean hasPadding) {
            return getFrameLengthMultiplier() * bitRate / sampleRate + (hasPadding ? 1 : 0);
        }
        
        private static boolean hasPadding(byte[] buffer, int offset) {
            return (buffer[offset + 2] & 0x02) != 0;
        }

    }
}
