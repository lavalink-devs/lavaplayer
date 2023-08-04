package com.sedmelluq.discord.lavaplayer.source.beam;

import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.stream.M3uStreamSegmentUrlProvider;
import com.sedmelluq.discord.lavaplayer.source.stream.MpegTsM3uStreamAudioTrack;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Audio track that handles processing Beam.pro tracks.
 */
public class BeamAudioTrack extends MpegTsM3uStreamAudioTrack {
    private static final Logger log = LoggerFactory.getLogger(BeamAudioTrack.class);

    private final BeamAudioSourceManager sourceManager;
    private final M3uStreamSegmentUrlProvider segmentUrlProvider;


    /**
     * @param trackInfo     Track info
     * @param sourceManager Source manager which was used to find this track
     */
    public BeamAudioTrack(AudioTrackInfo trackInfo, BeamAudioSourceManager sourceManager) {
        super(trackInfo);

        this.sourceManager = sourceManager;
        this.segmentUrlProvider = new BeamSegmentUrlProvider(getChannelId());
    }

    @Override
    protected M3uStreamSegmentUrlProvider getSegmentUrlProvider() {
        return segmentUrlProvider;
    }

    @Override
    protected HttpInterface getHttpInterface() {
        return sourceManager.getHttpInterface();
    }

    @Override
    public void process(LocalAudioTrackExecutor localExecutor) throws Exception {
        log.debug("Starting to play Beam channel {}.", getChannelUrl());

        super.process(localExecutor);
    }

    @Override
    protected AudioTrack makeShallowClone() {
        return new BeamAudioTrack(trackInfo, sourceManager);
    }

    @Override
    public AudioSourceManager getSourceManager() {
        return sourceManager;
    }

    private String getChannelId() {
        return trackInfo.identifier.substring(0, trackInfo.identifier.indexOf('|'));
    }

    private String getChannelUrl() {
        return trackInfo.identifier.substring(trackInfo.identifier.lastIndexOf('|') + 1);
    }
}
