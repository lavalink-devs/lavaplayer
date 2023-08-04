package com.sedmelluq.discord.lavaplayer.source.youtube;

import org.apache.http.entity.ContentType;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Describes an available media format for a track
 */
public class YoutubeTrackFormat {
    private final YoutubeFormatInfo info;
    private final ContentType type;
    private final long bitrate;
    private final long contentLength;
    private final long audioChannels;
    private final String url;
    private final String nParameter;
    private final String signature;
    private final String signatureKey;
    private final boolean defaultAudioTrack;

    /**
     * @param type          Mime type of the format
     * @param bitrate       Bitrate of the format
     * @param contentLength Length in bytes of the media
     * @param audioChannels Number of audio channels
     * @param url           Base URL for the playback of this format
     * @param nParameter    n parameter for this format
     * @param signature     Cipher signature for this format
     * @param signatureKey  The key to use for deciphered signature in the final playback URL
     */
    public YoutubeTrackFormat(
        ContentType type,
        long bitrate,
        long contentLength,
        long audioChannels,
        String url,
        String nParameter,
        String signature,
        String signatureKey,
        boolean isDefaultAudioTrack
    ) {
        this.info = YoutubeFormatInfo.get(type);
        this.type = type;
        this.bitrate = bitrate;
        this.contentLength = contentLength;
        this.audioChannels = audioChannels;
        this.url = url;
        this.nParameter = nParameter;
        this.signature = signature;
        this.signatureKey = signatureKey;
        this.defaultAudioTrack = isDefaultAudioTrack;
    }

    /**
     * @return Format container and codec info
     */
    public YoutubeFormatInfo getInfo() {
        return info;
    }

    /**
     * @return Mime type of the format
     */
    public ContentType getType() {
        return type;
    }

    /**
     * @return Bitrate of the format
     */
    public long getBitrate() {
        return bitrate;
    }

    /**
     * @return Count of audio channels in format
     */
    public long getAudioChannels() {
        return audioChannels;
    }

    /**
     * @return Base URL for the playback of this format
     */
    public URI getUrl() {
        try {
            return new URI(url);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return Length in bytes of the media
     */
    public long getContentLength() {
        return contentLength;
    }

    /**
     * @return n parameter for this format
     */
    public String getNParameter() {
        return nParameter;
    }

    /**
     * @return Cipher signature for this format
     */
    public String getSignature() {
        return signature;
    }

    /**
     * @return The key to use for deciphered signature in the final playback URL
     */
    public String getSignatureKey() {
        return signatureKey;
    }

    /**
     * @return Whether this format contains an audio track that is used by default.
     */
    public boolean isDefaultAudioTrack() {
        return defaultAudioTrack;
    }
}
