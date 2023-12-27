package com.sedmelluq.discord.lavaplayer.demo;

import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat;
import com.sedmelluq.discord.lavaplayer.format.AudioPlayerInputStream;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.FunctionalResultHandler;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.TrackMarker;
import com.sedmelluq.discord.lavaplayer.track.TrackMarkerHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats.COMMON_PCM_S16_BE;

public class LocalPlayerDemo {
    private static final Logger log = LoggerFactory.getLogger(LocalPlayerDemo.class);

    private static final long CROSSFADE_BEGIN = TimeUnit.SECONDS.toMillis(5);
    private static final long CROSSFADE_PRELOAD = CROSSFADE_BEGIN + TimeUnit.SECONDS.toMillis(3);

    public static void main(String[] args) throws LineUnavailableException, IOException {
        AudioPlayerManager manager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(manager);
        manager.getConfiguration().setOutputFormat(COMMON_PCM_S16_BE);

        AudioPlayer player = manager.createPlayer();

        manager.loadItem("ytsearch: zmvgDMe5Wxo", new FunctionalResultHandler(null, playlist -> {
            AudioTrack audioTrack = playlist.getTracks().get(0);

            applyMakers(audioTrack);

            player.playTrack(audioTrack);
        }, null, null));

        AudioDataFormat format = manager.getConfiguration().getOutputFormat();
        AudioInputStream stream = AudioPlayerInputStream.createStream(player, format, 10000L, false);
        SourceDataLine.Info info = new DataLine.Info(SourceDataLine.class, stream.getFormat());
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);

        line.open(stream.getFormat());
        line.start();

        byte[] buffer = new byte[COMMON_PCM_S16_BE.maximumChunkSize()];
        int chunkSize;

        while ((chunkSize = stream.read(buffer)) >= 0) {
            line.write(buffer, 0, chunkSize);
        }
    }

    private static void applyMakers(AudioTrack track) {
        final TrackMarkerHandler xfadeLoadHandler = (TrackMarkerHandler.MarkerState state) -> {
            if (state == TrackMarkerHandler.MarkerState.REACHED) {
                log.info("Fade begin handler has been reached");
            }
        };

        final TrackMarkerHandler xfadeBufferHandler = (TrackMarkerHandler.MarkerState state) -> {
            if (state == TrackMarkerHandler.MarkerState.REACHED) {
                log.info("Buffer begin handler has been reached");
            }
        };

        track.addMarker(new TrackMarker(track.getDuration() - CROSSFADE_BEGIN, xfadeLoadHandler));
        track.addMarker(new TrackMarker(track.getDuration() - CROSSFADE_PRELOAD, xfadeBufferHandler));
    }
}
