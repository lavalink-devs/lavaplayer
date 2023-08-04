package com.sedmelluq.discord.lavaplayer.track;

import com.sedmelluq.discord.lavaplayer.container.MediaContainerDescriptor;
import com.sedmelluq.discord.lavaplayer.track.info.AudioTrackInfoProvider;

/**
 * An audio item which refers to an unloaded audio item. Source managers can return this to indicate a redirection,
 * which means that the item referred to in it is loaded instead.
 */
public class AudioReference implements AudioItem, AudioTrackInfoProvider {
    public static final AudioReference NO_TRACK = new AudioReference(null, null, null);

    /**
     * The identifier of the other item.
     */
    public final String identifier;
    /**
     * The title of the other item, if known.
     */
    public final String title;
    /**
     * Known probe and probe settings of the item to be loaded.
     */
    public final MediaContainerDescriptor containerDescriptor;

    /**
     * @param identifier The identifier of the other item.
     * @param title      The title of the other item, if known.
     */
    public AudioReference(String identifier, String title) {
        this(identifier, title, null);
    }

    /**
     * @param identifier The identifier of the other item.
     * @param title      The title of the other item, if known.
     */
    public AudioReference(String identifier, String title, MediaContainerDescriptor containerDescriptor) {
        this.identifier = identifier;
        this.title = title;
        this.containerDescriptor = containerDescriptor;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getAuthor() {
        return null;
    }

    @Override
    public Long getLength() {
        return null;
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public String getUri() {
        return identifier;
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
