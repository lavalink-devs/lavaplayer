package com.sedmelluq.discord.lavaplayer.source.yamusic;

import com.sedmelluq.discord.lavaplayer.container.mp3.Mp3AudioTrack;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.tools.io.PersistentHttpStream;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.DelegatedAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

/**
 * Audio track that handles processing Yandex Music tracks.
 */
public class YandexMusicAudioTrack extends DelegatedAudioTrack {
    private static final Logger log = LoggerFactory.getLogger(YandexMusicAudioTrack.class);

    private final YandexMusicAudioSourceManager sourceManager;

    /**
     * @param trackInfo     Track info
     * @param sourceManager Source manager which was used to find this track
     */
    public YandexMusicAudioTrack(AudioTrackInfo trackInfo, YandexMusicAudioSourceManager sourceManager) {
        super(trackInfo);
        this.sourceManager = sourceManager;
    }

    @Override
    public void process(LocalAudioTrackExecutor localExecutor) throws Exception {
        try (HttpInterface httpInterface = sourceManager.getHttpInterface()) {
            String trackMediaUrl = sourceManager.getDirectUrlLoader().getDirectUrl(trackInfo.identifier, "mp3");
            log.debug("Starting Yandex Music track from URL: {}", trackMediaUrl);
            try (PersistentHttpStream stream = new PersistentHttpStream(httpInterface, new URI(trackMediaUrl), null)) {
                processDelegate(new Mp3AudioTrack(trackInfo, stream), localExecutor);
            }
        }
    }

    @Override
    public AudioTrack makeClone() {
        return new YandexMusicAudioTrack(trackInfo, sourceManager);
    }

    @Override
    public AudioSourceManager getSourceManager() {
        return sourceManager;
    }
}
