package com.sedmelluq.discord.lavaplayer.source.nico;

import com.sedmelluq.discord.lavaplayer.container.mpeg.MpegAudioTrack;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.tools.io.PersistentHttpStream;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.DelegatedAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Audio track that handles processing NicoNico tracks.
 */
public class NicoAudioTrack extends DelegatedAudioTrack {
    private static final Logger log = LoggerFactory.getLogger(NicoAudioTrack.class);

    private final NicoAudioSourceManager sourceManager;

    private String heartbeatUrl;
    private int heartbeatIntervalMs;
    private String initialHeartbeatPayload;

    /**
     * @param trackInfo     Track info
     * @param sourceManager Source manager which was used to find this track
     */
    public NicoAudioTrack(AudioTrackInfo trackInfo, NicoAudioSourceManager sourceManager) {
        super(trackInfo);

        this.sourceManager = sourceManager;
    }

    @Override
    public void process(LocalAudioTrackExecutor localExecutor) throws Exception {
        try (HttpInterface httpInterface = sourceManager.getHttpInterface()) {
            String playbackUrl = loadPlaybackUrl(httpInterface);

            log.debug("Starting NicoNico track from URL: {}", playbackUrl);

            try (HeartbeatingHttpStream stream = new HeartbeatingHttpStream(
                httpInterface,
                new URI(playbackUrl),
                null,
                heartbeatUrl,
                heartbeatIntervalMs,
                initialHeartbeatPayload
            )) {
                processDelegate(new MpegAudioTrack(trackInfo, stream), localExecutor);
            }
        }
    }

    private JsonBrowser loadVideoMainPage(HttpInterface httpInterface) throws IOException {
        try (CloseableHttpResponse response = httpInterface.execute(new HttpGet(trackInfo.uri))) {
            HttpClientTools.assertSuccessWithContent(response, "video main page");

            Document mainPage = Jsoup.parse(response.getEntity().getContent(), StandardCharsets.UTF_8.name(), "");
            String watchData = mainPage.getElementById("js-initial-watch-data").attr("data-api-data");

            return JsonBrowser.parse(watchData).get("media").get("delivery").get("movie").get("session");
        }
    }

    private String loadPlaybackUrl(HttpInterface httpInterface) throws IOException {
        JSONObject watchData = processJSON(loadVideoMainPage(httpInterface));

        HttpPost request = new HttpPost("https://api.dmc.nico/api/sessions?_format=json");
        request.addHeader("Host", "api.dmc.nico");
        request.addHeader("Connection", "keep-alive");
        request.addHeader("Content-Type", "application/json");
        request.addHeader("Origin", "https://www.nicovideo.jp");
        request.setEntity(new StringEntity(watchData.toString()));

        try (CloseableHttpResponse response = httpInterface.execute(request)) {
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode != HttpStatus.SC_CREATED) {
                throw new IOException("Unexpected status code from playback parameters page: " + statusCode);
            }

            JsonBrowser info = JsonBrowser.parse(response.getEntity().getContent()).get("data");
            JsonBrowser session = info.get("session");

            heartbeatUrl = "https://api.dmc.nico/api/sessions/" + session.get("id").text() + "?_format=json&_method=PUT";
            heartbeatIntervalMs = session.get("keep_method").get("heartbeat").get("lifetime").asInt(120000) - 5000;
            initialHeartbeatPayload = info.format();

            return session.get("content_uri").text();
        }
    }

    private JSONObject processJSON(JsonBrowser input) {
        JSONObject lifetime = new JSONObject().put("lifetime", input.get("heartbeatLifetime").asLong(120000));
        JSONObject heartbeat = new JSONObject().put("heartbeat", lifetime);

        List<String> videos = input.get("videos").values().stream()
            .map(JsonBrowser::text)
            .collect(Collectors.toList());

        List<String> audios = input.get("audios").values().stream()
            .map(JsonBrowser::text)
            .collect(Collectors.toList());

        JSONObject srcIds = new JSONObject()
            .put("video_src_ids", videos)
            .put("audio_src_ids", audios);

        JSONObject srcIdToMux = new JSONObject().put("src_id_to_mux", srcIds);
        JSONArray array = new JSONArray().put(srcIdToMux);
        JSONObject contentSrcIds = new JSONObject().put("content_src_ids", array);
        JSONArray contentSrcIdSets = new JSONArray().put(contentSrcIds);

        JsonBrowser url = input.get("urls").index(0);
        boolean useWellKnownPort = url.get("isWellKnownPort").asBoolean(false);
        boolean useSsl = url.get("isSsl").asBoolean(false);

        JSONObject httpDownloadParameters = new JSONObject()
            .put("use_well_known_port", useWellKnownPort ? "yes" : "no")
            .put("use_ssl", useSsl ? "yes" : "no");

        JSONObject innerParameters = new JSONObject()
            .put("http_output_download_parameters", httpDownloadParameters);

        JSONObject httpParameters = new JSONObject().put("parameters", innerParameters);
        JSONObject outerParameters = new JSONObject().put("http_parameters", httpParameters);

        JSONObject protocol = new JSONObject()
            .put("name", "http")
            .put("parameters", outerParameters);

        JSONObject sessionOperationAuthBySignature = new JSONObject()
            .put("token", input.get("token").text())
            .put("signature", input.get("signature").text());

        JSONObject sessionOperationAuth = new JSONObject()
            .put("session_operation_auth_by_signature", sessionOperationAuthBySignature);

        JSONObject contentAuth = new JSONObject()
            .put("auth_type", input.get("authTypes").get("http").text())
            .put("content_key_timeout", input.get("contentKeyTimeout").asLong(120000))
            .put("service_id", "nicovideo")
            .put("service_user_id", input.get("serviceUserId").text());

        JSONObject clientInfo = new JSONObject().put("player_id", input.get("playerId").text());

        JSONObject session = new JSONObject()
            .put("content_type", "movie")
            .put("timing_constraint", "unlimited")
            .put("recipe_id", input.get("recipeId").text())
            .put("content_id", input.get("contentId").text())
            .put("keep_method", heartbeat)
            .put("content_src_id_sets", contentSrcIdSets)
            .put("protocol", protocol)
            .put("session_operation_auth", sessionOperationAuth)
            .put("content_auth", contentAuth)
            .put("client_info", clientInfo);

        return new JSONObject().put("session", session);
    }

    @Override
    protected AudioTrack makeShallowClone() {
        return new NicoAudioTrack(trackInfo, sourceManager);
    }

    @Override
    public AudioSourceManager getSourceManager() {
        return sourceManager;
    }
}
