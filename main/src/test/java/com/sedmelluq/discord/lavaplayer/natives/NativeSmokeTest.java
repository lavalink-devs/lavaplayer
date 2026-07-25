package com.sedmelluq.discord.lavaplayer.natives;

import com.sedmelluq.discord.lavaplayer.natives.aac.AacDecoder;
import com.sedmelluq.discord.lavaplayer.natives.mp3.Mp3Decoder;
import com.sedmelluq.discord.lavaplayer.natives.opus.OpusDecoder;
import com.sedmelluq.discord.lavaplayer.natives.samplerate.SampleRateConverter;
import com.sedmelluq.discord.lavaplayer.natives.vorbis.VorbisDecoder;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.local.LocalAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class NativeSmokeTest {

    @BeforeAll
    static void checkNativesAvailable() {
        assumeTrue(
            NativeSmokeTest.class.getResource("/natives") != null,
            "Native binaries not available, skipping smoke tests"
        );
    }

    @Test
    void connectorNativeLibraryLoads() {
        assertDoesNotThrow(ConnectorNativeLibLoader::loadConnectorLibrary);
    }

    @Test
    void mp3DecoderCreatesAndCloses() {
        assertDoesNotThrow(() -> {
            Mp3Decoder decoder = new Mp3Decoder();
            decoder.close();
        });
    }

    @Test
    void opusDecoderCreatesAndCloses() {
        assertDoesNotThrow(() -> {
            OpusDecoder decoder = new OpusDecoder(48000, 2);
            decoder.close();
        });
    }

    @Test
    void aacDecoderCreatesAndCloses() {
        assertDoesNotThrow(() -> {
            AacDecoder decoder = new AacDecoder();
            decoder.close();
        });
    }

    @Test
    void vorbisDecoderCreatesAndCloses() {
        assertDoesNotThrow(() -> {
            VorbisDecoder decoder = new VorbisDecoder();
            decoder.close();
        });
    }

    @Test
    void sampleRateConverterCreatesAndCloses() {
        assertDoesNotThrow(() -> {
            SampleRateConverter converter = new SampleRateConverter(SampleRateConverter.ResamplingType.LINEAR, 2, 44100, 48000);
            converter.close();
        });
    }

    @Test
    void localFilePlayback(@TempDir Path tempDir) throws Exception {
        File wavFile = generateTestWav(tempDir.resolve("test.wav").toFile());

        AudioPlayerManager manager = new DefaultAudioPlayerManager();
        try {
            manager.registerSourceManager(new LocalAudioSourceManager());

            AudioTrack track = (AudioTrack) manager.loadItemSync(wavFile.getAbsolutePath());
            assertNotNull(track, "Track should be loaded from local WAV file");
            assertTrue(track.getDuration() > 0, "Track duration should be positive");

            AudioPlayer player = manager.createPlayer();
            player.playTrack(track);

            int framesRead = 0;
            for (int i = 0; i < 50; i++) {
                try {
                    AudioFrame frame = player.provide(500, TimeUnit.MILLISECONDS);
                    if (frame == null) break;
                    framesRead++;
                } catch (TimeoutException e) {
                    break;
                }
            }

            assertTrue(framesRead > 0, "Should have read at least one audio frame");

            player.stopTrack();
        } finally {
            manager.shutdown();
        }
    }

    private static File generateTestWav(File output) throws Exception {
        float sampleRate = 44100;
        int channels = 2;
        int durationMs = 500;
        int totalSamples = (int) (sampleRate * durationMs / 1000);

        byte[] data = new byte[totalSamples * channels * 2];
        for (int i = 0; i < totalSamples; i++) {
            short sample = (short) (Short.MAX_VALUE * 0.5 * Math.sin(2 * Math.PI * 440 * i / sampleRate));
            for (int ch = 0; ch < channels; ch++) {
                int offset = (i * channels + ch) * 2;
                data[offset] = (byte) (sample & 0xFF);
                data[offset + 1] = (byte) ((sample >> 8) & 0xFF);
            }
        }

        AudioFormat format = new AudioFormat(sampleRate, 16, channels, true, false);
        AudioInputStream stream = new AudioInputStream(
            new ByteArrayInputStream(data), format, totalSamples
        );
        AudioSystem.write(stream, AudioFileFormat.Type.WAVE, output);
        return output;
    }
}
