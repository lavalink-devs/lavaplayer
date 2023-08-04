package com.sedmelluq.discord.lavaplayer.source.youtube;

import com.sedmelluq.discord.lavaplayer.source.youtube.format.*;
import com.sedmelluq.discord.lavaplayer.tools.*;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeConstants.WATCH_URL_PREFIX;
import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.COMMON;
import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;
import static com.sedmelluq.discord.lavaplayer.tools.Units.DURATION_MS_UNKNOWN;

public class DefaultYoutubeTrackDetails implements YoutubeTrackDetails {
    private static final Logger log = LoggerFactory.getLogger(DefaultYoutubeTrackDetails.class);

    private static final YoutubeTrackFormatExtractor[] FORMAT_EXTRACTORS = new YoutubeTrackFormatExtractor[]{
        new LegacyAdaptiveFormatsExtractor(),
        new StreamingDataFormatsExtractor(),
        new LegacyDashMpdFormatsExtractor(),
        new LegacyStreamMapFormatsExtractor()
    };

    private final String videoId;
    private final YoutubeTrackJsonData data;

    public DefaultYoutubeTrackDetails(String videoId, YoutubeTrackJsonData data) {
        this.videoId = videoId;
        this.data = data;
    }

    @Override
    public AudioTrackInfo getTrackInfo() {
        return loadTrackInfo();
    }

    @Override
    public List<YoutubeTrackFormat> getFormats(HttpInterface httpInterface, YoutubeSignatureResolver signatureResolver) {
        try {
            return loadTrackFormats(httpInterface, signatureResolver);
        } catch (Exception e) {
            throw ExceptionTools.toRuntimeException(e);
        }
    }

    @Override
    public String getPlayerScript() {
        return data.playerScriptUrl;
    }

    private List<YoutubeTrackFormat> loadTrackFormats(
        HttpInterface httpInterface,
        YoutubeSignatureResolver signatureResolver
    ) {
        for (YoutubeTrackFormatExtractor extractor : FORMAT_EXTRACTORS) {
            List<YoutubeTrackFormat> formats = extractor.extract(data, httpInterface, signatureResolver);

            if (!formats.isEmpty()) {
                return formats;
            }
        }

        log.warn(
            "Video {} with no detected format field, response {} polymer {}",
            videoId,
            data.playerResponse.format(),
            data.polymerArguments.format()
        );

        throw new FriendlyException("Unable to play this YouTube track.", SUSPICIOUS,
            new IllegalStateException("No track formats found."));
    }

    private AudioTrackInfo loadTrackInfo() {
        JsonBrowser playabilityStatus = data.playerResponse.get("playabilityStatus");

        if ("ERROR".equals(playabilityStatus.get("status").text())) {
            throw new FriendlyException(playabilityStatus.get("reason").text(), COMMON, null);
        }

        JsonBrowser videoDetails = data.playerResponse.get("videoDetails");

        if (videoDetails.isNull()) {
            return loadLegacyTrackInfo();
        }

        TemporalInfo temporalInfo = TemporalInfo.fromRawData(
            !playabilityStatus.get("liveStreamability").isNull(),
            videoDetails.get("lengthSeconds"),
            false
        );

        return buildTrackInfo(videoId, videoDetails.get("title").text(), videoDetails.get("author").text(), temporalInfo, ThumbnailTools.getYouTubeThumbnail(videoDetails, videoId));
    }

    private AudioTrackInfo loadLegacyTrackInfo() {
        JsonBrowser args = data.polymerArguments;

        if ("fail".equals(args.get("status").text())) {
            throw new FriendlyException(args.get("reason").text(), COMMON, null);
        }

        TemporalInfo temporalInfo = TemporalInfo.fromRawData(
            "1".equals(args.get("live_playback").text()),
            args.get("length_seconds"),
            true
        );

        return buildTrackInfo(videoId, args.get("title").text(), args.get("author").text(), temporalInfo, ThumbnailTools.getYouTubeThumbnail(args, videoId));
    }

    private AudioTrackInfo buildTrackInfo(String videoId, String title, String uploader, TemporalInfo temporalInfo, String thumbnail) {
        return new AudioTrackInfo(title, uploader, temporalInfo.durationMillis, videoId, temporalInfo.isActiveStream,
            WATCH_URL_PREFIX + videoId, thumbnail, null);
    }

    private static class TemporalInfo {
        public final boolean isActiveStream;
        public final long durationMillis;

        private TemporalInfo(boolean isActiveStream, long durationMillis) {
            this.isActiveStream = isActiveStream;
            this.durationMillis = durationMillis;
        }

        public static TemporalInfo fromRawData(boolean wasLiveStream, JsonBrowser durationSecondsField, boolean legacy) {
            long durationValue = durationSecondsField.asLong(0L);
            boolean isActiveStream;
            if (wasLiveStream && !legacy) {
                // Premiers have total duration info field but acting as usual stream so when we try play it we don't know
                // current position of it since YT don't provide such info so assume duration is unknown.
                isActiveStream = true;
                durationValue = 0;
            } else {
                // VODs are not really live streams, even though that field in JSON claims they are. If it is actually live, then
                // duration is also missing or 0.
                isActiveStream = wasLiveStream && durationValue == 0;
            }

            return new TemporalInfo(
                isActiveStream,
                durationValue == 0 ? DURATION_MS_UNKNOWN : Units.secondsToMillis(durationValue)
            );
        }
    }
}
