package com.sedmelluq.discord.lavaplayer.source.soundcloud;

import com.sedmelluq.discord.lavaplayer.container.mp3.Mp3AudioTrack;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.tools.io.PersistentHttpStream;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.DelegatedAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;

/**
 * Audio track that handles processing SoundCloud tracks.
 */
public class SoundCloudAudioTrack extends DelegatedAudioTrack {
    private static final Logger log = LoggerFactory.getLogger(SoundCloudAudioTrack.class);

    private final SoundCloudAudioSourceManager sourceManager;

    /**
     * @param trackInfo     Track info
     * @param sourceManager Source manager which was used to find this track
     */
    public SoundCloudAudioTrack(AudioTrackInfo trackInfo, SoundCloudAudioSourceManager sourceManager) {
        super(trackInfo);

        this.sourceManager = sourceManager;
    }

    @Override
    public void process(LocalAudioTrackExecutor localExecutor) throws Exception {
        try (HttpInterface httpInterface = sourceManager.getHttpInterface()) {
            playFromIdentifier(httpInterface, trackInfo.identifier, false, localExecutor);
        }
    }

    private void playFromIdentifier(
        HttpInterface httpInterface,
        String identifier,
        boolean recursion,
        LocalAudioTrackExecutor localExecutor
    ) throws Exception {
        SoundCloudM3uInfo m3uInfo = sourceManager.getFormatHandler().getM3uInfo(identifier);

        if (m3uInfo != null) {
            processDelegate(new SoundCloudM3uAudioTrack(trackInfo, httpInterface, m3uInfo), localExecutor);
            return;
        }

        String mp3LookupUrl = sourceManager.getFormatHandler().getMp3LookupUrl(identifier);

        if (mp3LookupUrl != null) {
            String playbackUrl = SoundCloudHelper.loadPlaybackUrl(httpInterface, identifier.substring(2));
            loadFromMp3Url(localExecutor, httpInterface, playbackUrl);
            return;
        }

        if (!recursion) {
            // Old "track ID" entry? Let's "load" it to get url.
            AudioTrack track = sourceManager.loadTrack(trackInfo.uri);
            playFromIdentifier(httpInterface, track.getIdentifier(), true, localExecutor);
        }
    }

    private void loadFromMp3Url(
        LocalAudioTrackExecutor localExecutor,
        HttpInterface httpInterface,
        String trackUrl
    ) throws Exception {
        log.debug("Starting SoundCloud track from URL: {}", trackUrl);

        try (PersistentHttpStream stream = new PersistentHttpStream(httpInterface, new URI(trackUrl), null)) {
            if (!HttpClientTools.isSuccessWithContent(stream.checkStatusCode())) {
                throw new IOException("Invalid status code for soundcloud stream: " + stream.checkStatusCode());
            }

            processDelegate(new Mp3AudioTrack(trackInfo, stream), localExecutor);
        }
    }

    @Override
    protected AudioTrack makeShallowClone() {
        return new SoundCloudAudioTrack(trackInfo, sourceManager);
    }

    @Override
    public AudioSourceManager getSourceManager() {
        return sourceManager;
    }
}
