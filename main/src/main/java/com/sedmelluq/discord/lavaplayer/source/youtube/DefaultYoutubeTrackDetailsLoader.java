package com.sedmelluq.discord.lavaplayer.source.youtube;

import com.sedmelluq.discord.lavaplayer.tools.DataFormatTools;
import com.sedmelluq.discord.lavaplayer.tools.ExceptionTools;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeConstants.CLIENT_SCREEN_EMBED;
import static com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeConstants.CLIENT_THIRD_PARTY_EMBED;
import static com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeConstants.PLAYER_PARAMS;
import static com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeConstants.PLAYER_URL;
import static com.sedmelluq.discord.lavaplayer.tools.ExceptionTools.throwWithDebugInfo;
import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.COMMON;
import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;
import static java.nio.charset.StandardCharsets.UTF_8;

public class DefaultYoutubeTrackDetailsLoader implements YoutubeTrackDetailsLoader {
    private static final Logger log = LoggerFactory.getLogger(DefaultYoutubeTrackDetailsLoader.class);

    private volatile CachedPlayerScript cachedPlayerScript = null;

    @Override
    public YoutubeTrackDetails loadDetails(HttpInterface httpInterface, String videoId, boolean requireFormats, YoutubeAudioSourceManager sourceManager) {
        try {
            return load(httpInterface, videoId, requireFormats, sourceManager);
        } catch (IOException e) {
            throw ExceptionTools.toRuntimeException(e);
        }
    }

    private YoutubeTrackDetails load(
        HttpInterface httpInterface,
        String videoId,
        boolean requireFormats,
        YoutubeAudioSourceManager sourceManager
    ) throws IOException {
        JsonBrowser mainInfo = loadTrackInfoFromInnertube(httpInterface, videoId, sourceManager, null);

        try {
            YoutubeTrackJsonData initialData = loadBaseResponse(mainInfo, httpInterface, videoId, sourceManager);

            if (initialData == null) {
                return null;
            }

            if (!videoId.equals(initialData.playerResponse.get("videoDetails").get("videoId").text())) {
                throw new FriendlyException("Video returned by YouTube isn't what was requested", COMMON,
                    new IllegalStateException(initialData.playerResponse.format()));
            }

            YoutubeTrackJsonData finalData = augmentWithPlayerScript(initialData, httpInterface, videoId, requireFormats);
            return new DefaultYoutubeTrackDetails(videoId, finalData);
        } catch (FriendlyException e) {
            throw e;
        } catch (Exception e) {
            throw throwWithDebugInfo(log, e, "Error when extracting data", "mainJson", mainInfo.format());
        }
    }

    protected YoutubeTrackJsonData loadBaseResponse(
        JsonBrowser mainInfo,
        HttpInterface httpInterface,
        String videoId,
        YoutubeAudioSourceManager sourceManager
    ) throws IOException {
        YoutubeTrackJsonData data = YoutubeTrackJsonData.fromMainResult(mainInfo);
        InfoStatus status = checkPlayabilityStatus(data.playerResponse, false);

        if (status == InfoStatus.DOES_NOT_EXIST) {
            return null;
        }

        if (status == InfoStatus.PREMIERE_TRAILER) {
            JsonBrowser trackInfo = loadTrackInfoFromInnertube(httpInterface, videoId, sourceManager, status);
            data = YoutubeTrackJsonData.fromMainResult(trackInfo
                .get("playabilityStatus")
                .get("errorScreen")
                .get("ypcTrailerRenderer")
                .get("unserializedPlayerResponse")
            );
            status = checkPlayabilityStatus(data.playerResponse, true);
        }

        if (status == InfoStatus.REQUIRES_LOGIN) {
            JsonBrowser trackInfo = loadTrackInfoFromInnertube(httpInterface, videoId, sourceManager, status);
            data = YoutubeTrackJsonData.fromMainResult(trackInfo);
            status = checkPlayabilityStatus(data.playerResponse, true);
        }

        if (status == InfoStatus.NON_EMBEDDABLE) {
            JsonBrowser trackInfo = loadTrackInfoFromInnertube(httpInterface, videoId, sourceManager, status);
            data = YoutubeTrackJsonData.fromMainResult(trackInfo);
            checkPlayabilityStatus(data.playerResponse, true);
        }

        return data;
    }

    protected InfoStatus checkPlayabilityStatus(JsonBrowser playerResponse, boolean secondCheck) {
        JsonBrowser statusBlock = playerResponse.get("playabilityStatus");

        if (statusBlock.isNull()) {
            throw new RuntimeException("No playability status block.");
        }

        String status = statusBlock.get("status").text();

        if (status == null) {
            throw new RuntimeException("No playability status field.");
        } else if ("OK".equals(status)) {
            return InfoStatus.INFO_PRESENT;
        } else if ("ERROR".equals(status)) {
            String errorReason = statusBlock.get("reason").text();

            if (errorReason.contains("This video is unavailable")) {
                return InfoStatus.DOES_NOT_EXIST;
            } else {
                throw new FriendlyException(errorReason, COMMON, null);
            }
        } else if ("UNPLAYABLE".equals(status)) {
            String unplayableReason = getUnplayableReason(statusBlock);

            if (unplayableReason.contains("Playback on other websites has been disabled by the video owner")) {
                return InfoStatus.NON_EMBEDDABLE;
            }

            throw new FriendlyException(unplayableReason, COMMON, null);
        } else if ("LOGIN_REQUIRED".equals(status)) {
            String loginReason = statusBlock.get("reason").text();

            if (loginReason.contains("This video is private")) {
                throw new FriendlyException("This is a private video.", COMMON, null);
            }

            if (loginReason.contains("This video may be inappropriate for some users") && secondCheck) {
                throw new FriendlyException("This video requires age verification.", SUSPICIOUS,
                    new IllegalStateException("You did not set email and password in YoutubeAudioSourceManager."));
            }

            return InfoStatus.REQUIRES_LOGIN;
        } else if ("CONTENT_CHECK_REQUIRED".equals(status)) {
            throw new FriendlyException(getUnplayableReason(statusBlock), COMMON, null);
        } else if ("LIVE_STREAM_OFFLINE".equals(status)) {
            if (!statusBlock.get("errorScreen").get("ypcTrailerRenderer").isNull()) {
                return InfoStatus.PREMIERE_TRAILER;
            }

            throw new FriendlyException(getUnplayableReason(statusBlock), COMMON, null);
        } else {
            throw new FriendlyException("This video cannot be viewed anonymously.", COMMON, null);
        }
    }

    protected enum InfoStatus {
        INFO_PRESENT,
        REQUIRES_LOGIN,
        DOES_NOT_EXIST,
        CONTENT_CHECK_REQUIRED,
        LIVE_STREAM_OFFLINE,
        PREMIERE_TRAILER,
        NON_EMBEDDABLE
    }

    protected String getUnplayableReason(JsonBrowser statusBlock) {
        JsonBrowser playerErrorMessage = statusBlock.get("errorScreen").get("playerErrorMessageRenderer");
        String unplayableReason = statusBlock.get("reason").text();

        if (!playerErrorMessage.get("subreason").isNull()) {
            JsonBrowser subreason = playerErrorMessage.get("subreason");

            if (!subreason.get("simpleText").isNull()) {
                unplayableReason = subreason.get("simpleText").text();
            } else if (!subreason.get("runs").isNull() && subreason.get("runs").isList()) {
                StringBuilder reasonBuilder = new StringBuilder();
                subreason.get("runs").values().forEach(
                    item -> reasonBuilder.append(item.get("text").text()).append('\n')
                );
                unplayableReason = reasonBuilder.toString();
            }
        }

        return unplayableReason;
    }

    protected JsonBrowser loadTrackInfoFromInnertube(
        HttpInterface httpInterface,
        String videoId,
        YoutubeAudioSourceManager sourceManager,
        InfoStatus infoStatus
    ) throws IOException {
        if (cachedPlayerScript == null) fetchScript(videoId, httpInterface);

        YoutubeSignatureCipher playerScriptTimestamp = sourceManager.getSignatureResolver().getExtractedScript(
            httpInterface,
            cachedPlayerScript.playerScriptUrl
        );
        HttpPost post = new HttpPost(PLAYER_URL);
        YoutubeClientConfig clientConfig;

        if (infoStatus == InfoStatus.PREMIERE_TRAILER) {
            // Android client gives encoded Base64 response to trailer which is also protobuf so we can't decode it
            clientConfig = YoutubeClientConfig.WEB.copy();
        } else if (infoStatus == InfoStatus.NON_EMBEDDABLE) {
            // Used when age restriction bypass failed, if we have valid auth then most likely this request will be successful
            clientConfig = YoutubeClientConfig.ANDROID.copy()
                .withRootField("params", PLAYER_PARAMS);
        } else if (infoStatus == InfoStatus.REQUIRES_LOGIN) {
            // Age restriction bypass
            clientConfig = YoutubeClientConfig.TV_EMBEDDED.copy();
        } else {
            // Default payload from what we start trying to get required data
            clientConfig = YoutubeClientConfig.ANDROID.copy()
                .withClientField("clientScreen", CLIENT_SCREEN_EMBED)
                .withThirdPartyEmbedUrl(CLIENT_THIRD_PARTY_EMBED)
                .withRootField("params", PLAYER_PARAMS);
        }

        clientConfig
            .withRootField("racyCheckOk", true)
            .withRootField("contentCheckOk", true)
            .withRootField("videoId", videoId)
            .withPlaybackSignatureTimestamp(playerScriptTimestamp.scriptTimestamp)
            .setAttribute(httpInterface);

        log.debug("Loading track info with payload: {}", clientConfig.toJsonString());

        post.setEntity(new StringEntity(clientConfig.toJsonString(), "UTF-8"));
        try (CloseableHttpResponse response = httpInterface.execute(post)) {
            HttpClientTools.assertSuccessWithContent(response, "video page response");

            String responseText = EntityUtils.toString(response.getEntity(), UTF_8);

            try {
                return JsonBrowser.parse(responseText);
            } catch (FriendlyException e) {
                throw e;
            } catch (Exception e) {
                throw new FriendlyException("Received unexpected response from YouTube.", SUSPICIOUS,
                    new RuntimeException("Failed to parse: " + responseText, e));
            }
        }
    }

    protected YoutubeTrackJsonData augmentWithPlayerScript(
        YoutubeTrackJsonData data,
        HttpInterface httpInterface,
        String videoId,
        boolean requireFormats
    ) throws IOException {
        long now = System.currentTimeMillis();

        if (data.playerScriptUrl != null) {
            cachedPlayerScript = new CachedPlayerScript(data.playerScriptUrl, now);
            return data;
        } else if (!requireFormats) {
            return data;
        }

        CachedPlayerScript cached = cachedPlayerScript;

        if (cached != null && cached.timestamp + 600000L >= now) {
            return data.withPlayerScriptUrl(cached.playerScriptUrl);
        }

        return data.withPlayerScriptUrl(fetchScript(videoId, httpInterface));
    }

    private String fetchScript(String videoId, HttpInterface httpInterface) throws IOException {
        long now = System.currentTimeMillis();

        try (CloseableHttpResponse response = httpInterface.execute(new HttpGet("https://www.youtube.com/embed/" + videoId))) {
            HttpClientTools.assertSuccessWithContent(response, "youtube embed video id");

            String responseText = EntityUtils.toString(response.getEntity());
            String encodedUrl = DataFormatTools.extractBetween(responseText, "\"jsUrl\":\"", "\"");

            if (encodedUrl == null) {
                throw throwWithDebugInfo(log, null, "no jsUrl found", "html", responseText);
            }

            String fetchedPlayerScript = JsonBrowser.parse("{\"url\":\"" + encodedUrl + "\"}").get("url").text();
            cachedPlayerScript = new CachedPlayerScript(fetchedPlayerScript, now);

            return fetchedPlayerScript;
        }
    }

    protected static class CachedPlayerScript {
        public final String playerScriptUrl;
        public final long timestamp;

        public CachedPlayerScript(String playerScriptUrl, long timestamp) {
            this.playerScriptUrl = playerScriptUrl;
            this.timestamp = timestamp;
        }
    }
}
