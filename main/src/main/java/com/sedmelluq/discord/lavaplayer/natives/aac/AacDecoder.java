package com.sedmelluq.discord.lavaplayer.natives.aac;

import com.sedmelluq.discord.lavaplayer.tools.io.BitStreamReader;
import com.sedmelluq.discord.lavaplayer.tools.io.BitStreamWriter;
import com.sedmelluq.discord.lavaplayer.tools.io.ByteBufferOutputStream;
import com.sedmelluq.lava.common.natives.NativeResourceHolder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

/**
 * A wrapper around the native methods of AacDecoder, which uses fdk-aac native library. Supports data with no transport
 * layer. The only AAC type verified to work with this is AAC_LC.
 */
public class AacDecoder extends NativeResourceHolder {
    private static final int[] SAMPLERATE_TABLE = { 96000, 88200, 64000, 48000, 44100,
        32000, 24000, 22050, 16000, 12000, 11025, 8000, 7350 };

    private static final int TRANSPORT_NONE = 0;

    // profiles
    public static final int AAC_LC = 2;
    public static final int SBR = 5; // HE-AAC
    public static final int PS = 29; // HE-AACv2

    private static final ShortBuffer NO_BUFFER = ByteBuffer.allocateDirect(0).asShortBuffer();

    private static final int ERROR_NOT_ENOUGH_BITS = 4098;
    private static final int ERROR_OUTPUT_BUFFER_TOO_SMALL = 8204;

    private final AacDecoderLibrary library;
    private final long instance;

    /**
     * Create a new decoder.
     */
    public AacDecoder() {
        library = AacDecoderLibrary.getInstance();
        instance = library.create(TRANSPORT_NONE);
    }

    /**
     * Configure the decoder. Must be called before the first decoding.
     *
     * @param objectType Audio object type as defined for Audio Specific Config: https://wiki.multimedia.cx/index.php?title=MPEG-4_Audio
     * @param frequency  Frequency of samples in Hz
     * @param channels   Number of channels.
     * @throws IllegalStateException If the decoder has already been closed.
     */
    public int configure(int objectType, int frequency, int channels) {
        int extensionFrequency = isSbrOrPs(objectType) ? frequency * 2 : frequency;
        int extensionProfile = isSbrOrPs(objectType) ? AAC_LC : objectType;

        long buffer = encodeConfiguration(objectType, frequency, channels, extensionFrequency, extensionProfile);
        return configureRaw(buffer);
    }

    /**
     * Configure the decoder. Must be called before the first decoding.
     *
     * @param config Raw ASC format configuration
     * @throws IllegalStateException If the decoder has already been closed.
     */
    public int configure(byte[] config) {
        if (config.length > 8) {
            throw new IllegalArgumentException("Cannot process a header larger than size 8");
        }

        long buffer = 0;
        for (int i = 0; i < config.length; i++) {
            // & 0xff converts to unsigned long. Thanks to Viztea for finding this
            buffer |= ((long) (config[i] & 0xff)) << (i << 3);
        }

        int ret = configureRaw(buffer);

        if (ret != 0) {
            try (ByteArrayInputStream stream = new ByteArrayInputStream(config)) {
                BitStreamReader reader = new BitStreamReader(stream);

                int objectType = reader.asInteger(5);
                int sampleRateIndex = reader.asInteger(4);
                int sampleRate = sampleRateIndex == 15 ? reader.asInteger(24) : SAMPLERATE_TABLE[sampleRateIndex];
                int channels = reader.asInteger(4);

                return configure(objectType, sampleRate, channels);
            } catch (IOException ignored) {

            }
        }

        return ret;
    }

    private synchronized int configureRaw(long buffer) {
        checkNotReleased();

        if (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) {
            buffer = Long.reverseBytes(buffer);
        }

        return library.configure(instance, buffer);
    }

    private static boolean isSbrOrPs(int objectType) {
        return objectType == SBR || objectType == PS;
    }

    private static long encodeConfiguration(int objectType,
                                            int frequency,
                                            int channels,
                                            int extensionFrequency,
                                            int extensionProfile) {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(8);
            buffer.order(ByteOrder.nativeOrder());
            BitStreamWriter bitWriter = new BitStreamWriter(new ByteBufferOutputStream(buffer));

            bitWriter.write(objectType, 5);

            int frequencyIndex = getFrequencyIndex(frequency);
            bitWriter.write(frequencyIndex, 4);

            if (frequencyIndex == 15) {
                bitWriter.write(frequency, 24);
            }

            bitWriter.write(channels, 4);

            if (isSbrOrPs(objectType)) {
                int extensionFrequencyIndex = getFrequencyIndex(extensionFrequency);
                bitWriter.write(extensionFrequencyIndex, 4);

                if (extensionFrequencyIndex == 15) {
                    bitWriter.write(extensionFrequency, 24);
                }

                bitWriter.write(extensionProfile, 5);
            }

            bitWriter.flush();
            buffer.clear();

            return buffer.getLong();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static int getFrequencyIndex(int frequency) {
        for (int i = 0; i < SAMPLERATE_TABLE.length; i++) {
            if (SAMPLERATE_TABLE[i] == frequency) {
                return i;
            }
        }

        return 15;
    }

    /**
     * Fill the internal decoding buffer with the bytes from the buffer. May consume less bytes than the buffer provides.
     *
     * @param buffer DirectBuffer which contains the bytes to be added. Position and limit are respected and position is
     *               updated as a result of this operation.
     * @return The number of bytes consumed from the provided buffer.
     * @throws IllegalArgumentException If the buffer is not a DirectBuffer.
     * @throws IllegalStateException    If the decoder has already been closed.
     */
    public synchronized int fill(ByteBuffer buffer) {
        checkNotReleased();

        if (!buffer.isDirect()) {
            throw new IllegalArgumentException("Buffer argument must be a direct buffer.");
        }

        int readBytes = library.fill(instance, buffer, buffer.position(), buffer.limit());
        if (readBytes < 0) {
            throw new IllegalStateException("Filling decoder failed with error " + (-readBytes));
        }

        buffer.position(buffer.position() + readBytes);
        return readBytes;
    }

    /**
     * Decode a frame of audio into the given buffer.
     *
     * @param buffer DirectBuffer of signed PCM samples where the decoded frame will be stored. The buffer size must be at
     *               least of size <code>frameSize * channels * 2</code>. Buffer position and limit are ignored and not
     *               updated.
     * @param flush  Whether all the buffered data should be flushed, set to true if no more input is expected.
     * @return True if the frame buffer was filled, false if there was not enough input for decoding a full frame.
     * @throws IllegalArgumentException If the buffer is not a DirectBuffer.
     * @throws IllegalStateException    If the decoding library returns an error other than running out of input data.
     * @throws IllegalStateException    If the decoder has already been closed.
     */
    public synchronized boolean decode(ShortBuffer buffer, boolean flush) {
        checkNotReleased();

        if (!buffer.isDirect()) {
            throw new IllegalArgumentException("Buffer argument must be a direct buffer.");
        }

        int result = library.decode(instance, buffer, buffer.capacity(), flush);
        if (result != 0 && result != ERROR_NOT_ENOUGH_BITS) {
            throw new IllegalStateException("Error from decoder " + result);
        }

        return result == 0;
    }

    /**
     * @return Correct stream info. The values passed to configure method do not account for SBR and PS and detecting
     * these is a part of the decoding process. If there was not enough input for decoding a full frame, null is
     * returned.
     * @throws IllegalStateException If the decoder result produced an unexpected error.
     */
    public synchronized StreamInfo resolveStreamInfo() {
        checkNotReleased();

        int result = library.decode(instance, NO_BUFFER, 0, false);

        if (result == ERROR_NOT_ENOUGH_BITS) {
            return null;
        } else if (result != ERROR_OUTPUT_BUFFER_TOO_SMALL) {
            throw new IllegalStateException("Expected decoding to halt, got: " + result);
        }

        long combinedValue = library.getStreamInfo(instance);
        if (combinedValue == 0) {
            throw new IllegalStateException("Native library failed to detect stream info.");
        }

        return new StreamInfo(
            (int) (combinedValue >>> 32L),
            (int) (combinedValue & 0xFFFF),
            (int) ((combinedValue >>> 16L) & 0xFFFF)
        );
    }

    @Override
    protected void freeResources() {
        library.destroy(instance);
    }

    /**
     * AAC stream information.
     */
    public static class StreamInfo {
        /**
         * Sample rate (adjusted to SBR) of the current stream.
         */
        public final int sampleRate;
        /**
         * Channel count (adjusted to PS) of the current stream.
         */
        public final int channels;
        /**
         * Number of samples per channel per frame.
         */
        public final int frameSize;

        /**
         * @param sampleRate Sample rate (adjusted to SBR) of the current stream.
         * @param channels   Channel count (adjusted to PS) of the current stream.
         * @param frameSize  Number of samples per channel per frame.
         */
        public StreamInfo(int sampleRate, int channels, int frameSize) {
            this.sampleRate = sampleRate;
            this.channels = channels;
            this.frameSize = frameSize;
        }
    }
}
