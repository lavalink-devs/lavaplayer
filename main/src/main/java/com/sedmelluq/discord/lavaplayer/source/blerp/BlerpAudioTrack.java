package com.sedmelluq.discord.lavaplayer.source.blerp;

import com.sedmelluq.discord.lavaplayer.container.mp3.Mp3AudioTrack;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.Units;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.tools.io.PersistentHttpStream;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.DelegatedAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

public class BlerpAudioTrack extends DelegatedAudioTrack {
    private static final Logger log = LoggerFactory.getLogger(BlerpAudioTrack.class);

    private final BlerpAudioSourceManager sourceManager;

    /**
     * @param trackInfo     Track info
     * @param sourceManager Source manager used to load this track
     */
    public BlerpAudioTrack(AudioTrackInfo trackInfo,
                           BlerpAudioSourceManager sourceManager) {
        super(trackInfo);
        this.sourceManager = sourceManager;
    }


    @Override
    public void process(LocalAudioTrackExecutor localExecutor) throws Exception {
        try (HttpInterface httpInterface = sourceManager.getHttpInterface()) {
            log.debug("Starting Blerp track from URL: {}", trackInfo.identifier);

            try (PersistentHttpStream inputStream =
                     new PersistentHttpStream(httpInterface,
                         new URI(trackInfo.identifier),
                         Units.CONTENT_LENGTH_UNKNOWN)) {
                processDelegate(new Mp3AudioTrack(trackInfo, inputStream), localExecutor);
            }
        }
    }

    @Override
    protected AudioTrack makeShallowClone() {
        return new BlerpAudioTrack(trackInfo, sourceManager);
    }

    @Override
    public AudioSourceManager getSourceManager() {
        return sourceManager;
    }
}
