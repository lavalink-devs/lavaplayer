package com.sedmelluq.discord.lavaplayer.source.soundcloud;

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;

import java.util.List;

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.COMMON;

public class DefaultSoundCloudFormatHandler implements SoundCloudFormatHandler {
    private static final FormatType[] TYPES = FormatType.values();

    @Override
    public SoundCloudTrackFormat chooseBestFormat(List<SoundCloudTrackFormat> formats) {
        for (FormatType type : TYPES) {
            for (SoundCloudTrackFormat format : formats) {
                if (type.matches(format)) {
                    return format;
                }
            }
        }

        if (!formats.isEmpty() && formats.stream().allMatch(DefaultSoundCloudFormatHandler::isEncrypted)) {
            throw new FriendlyException("This track is DRM-protected by SoundCloud and cannot be played.", COMMON, null);
        }

        throw new RuntimeException("Did not detect any supported formats");
    }

    @Override
    public String buildFormatIdentifier(SoundCloudTrackFormat format) {
        for (FormatType type : TYPES) {
            if (type.matches(format)) {
                return type.prefix + format.getLookupUrl();
            }
        }

        return "X:" + format.getLookupUrl();
    }

    @Override
    public SoundCloudM3uInfo getM3uInfo(String identifier) {
        if (identifier.startsWith(FormatType.TYPE_M3U_OPUS.prefix)) {
            return new SoundCloudM3uInfo(identifier.substring(2), SoundCloudOpusSegmentDecoder::new);
        } else if (identifier.startsWith(FormatType.TYPE_M3U_MP3.prefix)) {
            return new SoundCloudM3uInfo(identifier.substring(2), SoundCloudMp3SegmentDecoder::new);
        } else if (identifier.startsWith(FormatType.TYPE_M3U_AAC.prefix)) {
            return new SoundCloudM3uInfo(identifier.substring(2), SoundCloudAacSegmentDecoder::new);
        }

        return null;
    }

    @Override
    public String getMp3LookupUrl(String identifier) {
        if (identifier.startsWith("M:")) {
            return identifier.substring(2);
        }

        return null;
    }

    private static boolean isEncrypted(SoundCloudTrackFormat format) {
        return format.getProtocol().contains("encrypted");
    }

    private static SoundCloudTrackFormat findFormat(List<SoundCloudTrackFormat> formats, FormatType type) {
        for (SoundCloudTrackFormat format : formats) {
            if (type.matches(format)) {
                return format;
            }
        }

        return null;
    }

    private enum FormatType {
        TYPE_M3U_OPUS("hls", "audio/ogg", "O:"),
        TYPE_M3U_MP3("hls", "audio/mpeg", "U:"),
        TYPE_SIMPLE_MP3("progressive", "audio/mpeg", "M:"),
        TYPE_M3U_AAC("hls", "audio/mp4", "A:");

        public final String protocol;
        public final String mimeType;
        public final String prefix;

        FormatType(String protocol, String mimeType, String prefix) {
            this.protocol = protocol;
            this.mimeType = mimeType;
            this.prefix = prefix;
        }

        public boolean matches(SoundCloudTrackFormat format) {
            // Mime type may carry parameters, for example: audio/mp4; codecs="mp4a.40.2"
            String formatMimeType = format.getMimeType();
            return protocol.equals(format.getProtocol())
                && (formatMimeType.equals(mimeType) || formatMimeType.startsWith(mimeType + ";"));
        }
    }
}
