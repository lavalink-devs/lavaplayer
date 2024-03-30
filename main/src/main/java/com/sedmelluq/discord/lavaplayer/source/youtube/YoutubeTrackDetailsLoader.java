package com.sedmelluq.discord.lavaplayer.source.youtube;

import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;

public interface YoutubeTrackDetailsLoader {
    YoutubeTrackDetails loadDetails(HttpInterface httpInterface,
                                    String videoId,
                                    boolean requireFormats,
                                    YoutubeAudioSourceManager sourceManager);

    default YoutubeTrackDetails loadDetails(HttpInterface httpInterface,
                                    String videoId,
                                    boolean requireFormats,
                                    YoutubeAudioSourceManager sourceManager,
                                    YoutubeClientConfig clientOverride) {
        // Implemented this way to be non-breaking. clientOverride will be ignored for implementations
        // that don't implement this override.
        return loadDetails(httpInterface, videoId, requireFormats, sourceManager);
    }
}
