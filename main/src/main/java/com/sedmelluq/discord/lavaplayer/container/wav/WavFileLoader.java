package com.sedmelluq.discord.lavaplayer.container.wav;

import com.sedmelluq.discord.lavaplayer.tools.io.SeekableInputStream;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioProcessingContext;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static com.sedmelluq.discord.lavaplayer.container.MediaContainerDetection.checkNextBytes;

/**
 * Loads either WAV header information or a WAV track provider from a stream.
 */
public class WavFileLoader {
    static final int[] WAV_RIFF_HEADER = new int[] { 0x52, 0x49, 0x46, 0x46, -1, -1, -1, -1, 0x57, 0x41, 0x56, 0x45 };
    static final byte[] FORMAT_SUBTYPE_PCM = { 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x10, 0x00, (byte) 0x80, 0x00, 0x00, (byte) 0xaa, 0x00, 0x38, (byte) 0x9b, 0x71 };

    private final SeekableInputStream inputStream;

    /**
     * @param inputStream Input stream to read the WAV data from. This must be positioned right before WAV RIFF header.
     */
    public WavFileLoader(SeekableInputStream inputStream) {
        this.inputStream = inputStream;
    }

    /**
     * Parses the headers of the file.
     *
     * @return Format description of the WAV file
     * @throws IOException On read error
     */
    public WavFileInfo parseHeaders() throws IOException {
        if (!checkNextBytes(inputStream, WAV_RIFF_HEADER, false)) {
            throw new IllegalStateException("Not a WAV header.");
        }

        InfoBuilder builder = new InfoBuilder();
        DataInput dataInput = new DataInputStream(inputStream);

        while (true) {
            String chunkName = readChunkName(dataInput);
            long chunkSize = Integer.toUnsignedLong(Integer.reverseBytes(dataInput.readInt()));

            if ("fmt ".equals(chunkName)) {
                int bytesRead = readFormatChunk(builder, dataInput);
                long chunkBytesRemaining = chunkSize - bytesRead;

                if (chunkBytesRemaining > 0) {
                    inputStream.skipFully(chunkBytesRemaining);
                }
            } else if ("data".equals(chunkName)) {
                builder.sampleAreaSize = chunkSize;
                builder.startOffset = inputStream.getPosition();
                return builder.build();
            } else {
                inputStream.skipFully(chunkSize);
            }
        }
    }

    private String readChunkName(DataInput dataInput) throws IOException {
        byte[] buffer = new byte[4];
        dataInput.readFully(buffer);
        return new String(buffer, StandardCharsets.US_ASCII);
    }

    private int readFormatChunk(InfoBuilder builder, DataInput dataInput) throws IOException {
        builder.setAudioFormat(Short.reverseBytes(dataInput.readShort()) & 0xFFFF);
        builder.channelCount = Short.reverseBytes(dataInput.readShort()) & 0xFFFF;
        builder.sampleRate = Integer.reverseBytes(dataInput.readInt());

        // Skip bitrate
        dataInput.readInt();

        builder.blockAlign = Short.reverseBytes(dataInput.readShort()) & 0xFFFF;
        builder.bitsPerSample = Short.reverseBytes(dataInput.readShort()) & 0xFFFF;

        if (builder.formatType == WaveFormatType.WAVE_FORMAT_EXTENSIBLE) {
            dataInput.skipBytes(8);
            byte[] subFormat = new byte[16];
            dataInput.readFully(subFormat);
            builder.subFormat = subFormat;
            return 40;
        }

        return 16;
    }

    /**
     * Initialise a WAV track stream.
     *
     * @param context Configuration and output information for processing
     * @return The WAV track stream which can produce frames.
     * @throws IOException On read error
     */
    public WavTrackProvider loadTrack(AudioProcessingContext context) throws IOException {
        return new WavTrackProvider(context, inputStream, parseHeaders());
    }

    private static class InfoBuilder {
        private int audioFormat;
        private WaveFormatType formatType;
        private byte[] subFormat;
        private int channelCount;
        private int sampleRate;
        private int bitsPerSample;
        private int blockAlign;
        private long sampleAreaSize;
        private long startOffset;

        private void setAudioFormat(int audioFormat) {
            this.audioFormat = audioFormat;
            this.formatType = WaveFormatType.getByCode(audioFormat);
        }

        private WavFileInfo build() {
            validateFormat();
            validateAlignment();

            return new WavFileInfo(channelCount, sampleRate, bitsPerSample, blockAlign, sampleAreaSize / blockAlign, startOffset);
        }

        private void validateFormat() {
            if (formatType == WaveFormatType.WAVE_FORMAT_UNKNOWN) {
                throw new IllegalStateException("Invalid audio format " + audioFormat + ", must be 1 (PCM) or 65534 (WAVE_FORMAT_EXTENSIBLE)");
            } else if (subFormat != null && !Arrays.equals(subFormat, FORMAT_SUBTYPE_PCM)) {
                throw new IllegalStateException("Invalid subformat " + Arrays.toString(subFormat));
            } else if (channelCount < 1 || channelCount > 16) {
                throw new IllegalStateException("Invalid channel count: " + channelCount);
            } else if (sampleRate < 100 || sampleRate > 384000) {
                throw new IllegalStateException("Invalid sample rate: " + sampleRate);
            } else if (bitsPerSample != 16 && bitsPerSample != 24 && bitsPerSample != 32) {
                throw new IllegalStateException("Unsupported bits per sample: " + bitsPerSample);
            }
        }

        private void validateAlignment() {
            int minimumBlockAlign = channelCount * (bitsPerSample >> 3);

            if (blockAlign < minimumBlockAlign || blockAlign > minimumBlockAlign + 32) {
                throw new IllegalStateException("Block align is not valid: " + blockAlign);
            } else if (blockAlign % (bitsPerSample >> 3) != 0) {
                throw new IllegalStateException("Block align is not a multiple of bits per sample: " + blockAlign);
            } else if (sampleAreaSize < 0) {
                throw new IllegalStateException("Negative sample area size: " + sampleAreaSize);
            }
        }
    }
}
