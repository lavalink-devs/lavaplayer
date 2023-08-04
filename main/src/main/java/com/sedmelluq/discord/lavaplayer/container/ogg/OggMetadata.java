package com.sedmelluq.discord.lavaplayer.container.ogg;

import com.sedmelluq.discord.lavaplayer.tools.Units;
import com.sedmelluq.discord.lavaplayer.track.info.AudioTrackInfoProvider;

import java.util.Collections;
import java.util.Map;

/**
 * Audio track info provider based on OGG metadata map.
 */
public class OggMetadata implements AudioTrackInfoProvider {
    public static final OggMetadata EMPTY = new OggMetadata(Collections.emptyMap(), Units.DURATION_MS_UNKNOWN);

    private static final String TITLE_FIELD = "TITLE";
    private static final String ARTIST_FIELD = "ARTIST";

    private final Map<String, String> tags;
    private final long length;

    /**
     * @param tags Map of OGG metadata with OGG-specific keys.
     */
    public OggMetadata(Map<String, String> tags, Long length) {
        this.tags = tags;
        this.length = length;
    }

    @Override
    public String getTitle() {
        return tags.get(TITLE_FIELD);
    }

    @Override
    public String getAuthor() {
        return tags.get(ARTIST_FIELD);
    }

    @Override
    public Long getLength() {
        return length;
    }

    @Override
    public String getIdentifier() {
        return null;
    }

    @Override
    public String getUri() {
        return null;
    }

    @Override
    public String getArtworkUrl() {
        return null;
    }

    @Override
    public String getISRC() {
        return null;
    }
}
