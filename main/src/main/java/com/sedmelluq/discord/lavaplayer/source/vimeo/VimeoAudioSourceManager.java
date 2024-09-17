package com.sedmelluq.discord.lavaplayer.source.vimeo;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.*;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpConfigurable;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterfaceManager;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;

/**
 * Audio source manager which detects Vimeo tracks by URL.
 */
public class VimeoAudioSourceManager implements AudioSourceManager, HttpConfigurable {
    private static final String TRACK_URL_REGEX = "^https?://vimeo.com/([0-9]+)(?:\\?.*|)$";
    private static final Pattern trackUrlPattern = Pattern.compile(TRACK_URL_REGEX);

    private final HttpInterfaceManager httpInterfaceManager;

    /**
     * Create an instance.
     */
    public VimeoAudioSourceManager() {
        httpInterfaceManager = HttpClientTools.createDefaultThreadLocalManager();
    }

    @Override
    public String getSourceName() {
        return "vimeo";
    }

    @Override
    public AudioItem loadItem(AudioPlayerManager manager, AudioReference reference) {
        Matcher trackUrl = trackUrlPattern.matcher(reference.identifier);

        if (!trackUrl.matches()) {
            return null;
        }

        try (HttpInterface httpInterface = httpInterfaceManager.getInterface()) {
            return loadVideoFromApi(httpInterface, trackUrl.group(1));
        } catch (IOException | URISyntaxException e) {
            throw new FriendlyException("Loading Vimeo track information failed.", SUSPICIOUS, e);
        }
    }

    @Override
    public boolean isTrackEncodable(AudioTrack track) {
        return true;
    }

    @Override
    public void encodeTrack(AudioTrack track, DataOutput output) throws IOException {
        // Nothing special to encode
    }

    @Override
    public AudioTrack decodeTrack(AudioTrackInfo trackInfo, DataInput input) throws IOException {
        return new VimeoAudioTrack(trackInfo, this);
    }

    @Override
    public void shutdown() {
        ExceptionTools.closeWithWarnings(httpInterfaceManager);
    }

    /**
     * @return Get an HTTP interface for a playing track.
     */
    public HttpInterface getHttpInterface() {
        return httpInterfaceManager.getInterface();
    }

    HttpInterfaceManager getHttpInterfaceManager() {
        return httpInterfaceManager;
    }

    @Override
    public void configureRequests(Function<RequestConfig, RequestConfig> configurator) {
        httpInterfaceManager.configureRequests(configurator);
    }

    @Override
    public void configureBuilder(Consumer<HttpClientBuilder> configurator) {
        httpInterfaceManager.configureBuilder(configurator);
    }

    JsonBrowser loadConfigJsonFromPageContent(String content) throws IOException {
        String configText = DataFormatTools.extractBetween(content, "window.vimeo.clip_page_config = ", "\n");

        if (configText != null) {
            return JsonBrowser.parse(configText);
        }

        return null;
    }

    private AudioItem loadFromTrackPage(HttpInterface httpInterface, String trackUrl) throws IOException {
        try (CloseableHttpResponse response = httpInterface.execute(new HttpGet(trackUrl))) {
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode == HttpStatus.SC_NOT_FOUND) {
                return AudioReference.NO_TRACK;
            } else if (!HttpClientTools.isSuccessWithContent(statusCode)) {
                throw new FriendlyException("Server responded with an error.", SUSPICIOUS,
                    new IllegalStateException("Response code is " + statusCode));
            }

            return loadTrackFromPageContent(trackUrl, IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8));
        }
    }

    private AudioTrack loadTrackFromPageContent(String trackUrl, String content) throws IOException {
        JsonBrowser config = loadConfigJsonFromPageContent(content);

        if (config == null) {
            throw new FriendlyException("Track information not found on the page.", SUSPICIOUS, null);
        }

        return new VimeoAudioTrack(new AudioTrackInfo(
            config.get("clip").get("title").text(),
            config.get("owner").get("display_name").text(),
            (long) (config.get("clip").get("duration").get("raw").as(Double.class) * 1000.0),
            trackUrl,
            false,
            trackUrl,
            config.get("thumbnail").get("src").text(),
            null
        ), this);
    }

    private AudioTrack loadVideoFromApi(HttpInterface httpInterface, String videoId) throws IOException, URISyntaxException {
        JsonBrowser videoData = getVideoFromApi(httpInterface, videoId);

        AudioTrackInfo info = new AudioTrackInfo(
            videoData.get("name").text(),
            videoData.get("uploader").get("name").textOrDefault("Unknown artist"),
            Units.secondsToMillis(videoData.get("duration").asLong(Units.DURATION_SEC_UNKNOWN)),
            videoId,
            false,
            "https://vimeo.com/" + videoId,
            videoData.get("pictures").get("base_link").text(),
            null
        );

        return new VimeoAudioTrack(info, this);
    }

    public JsonBrowser getVideoFromApi(HttpInterface httpInterface, String videoId) throws IOException, URISyntaxException {
        String jwt = getApiJwt(httpInterface);

        URIBuilder builder = new URIBuilder("https://api.vimeo.com/videos/" + videoId);
        // adding `play` to the fields achieves the same thing as requesting the config_url, but with one less request.
        // maybe we should consider using that instead? Need to figure out what the difference is, if any.
        builder.setParameter("fields", "config_url,name,uploader.name,duration,pictures");

        HttpUriRequest request = new HttpGet(builder.build());
        request.setHeader("Authorization", "jwt " + jwt);
        request.setHeader("Accept", "application/json");

        try (CloseableHttpResponse response = httpInterface.execute(request)) {
            HttpClientTools.assertSuccessWithContent(response, "fetch video api");
            return JsonBrowser.parse(response.getEntity().getContent());
        }
    }

    public PlaybackFormat getPlaybackFormat(HttpInterface httpInterface, String configUrl) throws IOException {
        try (CloseableHttpResponse response = httpInterface.execute(new HttpGet(configUrl))) {
            HttpClientTools.assertSuccessWithContent(response, "fetch playback formats");

            JsonBrowser json = JsonBrowser.parse(response.getEntity().getContent());

            // {"dash", "hls", "progressive"}
            // N.B. opus is referenced in some of the URLs, but I don't see any formats offering opus audio codec.
            // Might be a gradual rollout so this may need revisiting.
            JsonBrowser files = json.get("request").get("files");

            if (!files.get("progressive").isNull()) {
                JsonBrowser progressive = files.get("progressive").index(0);

                if (!progressive.isNull()) {
                    return new PlaybackFormat(progressive.get("url").text(), false);
                }
            }

            if (!files.get("hls").isNull()) {
                JsonBrowser hls = files.get("hls");
                // ["akfire_interconnect_quic", "fastly_skyfire"]
                JsonBrowser cdns = hls.get("cdns");
                return new PlaybackFormat(cdns.get(hls.get("default_cdn").text()).get("url").text(), true);
            }

            throw new RuntimeException("No supported formats");
        }
    }

    private String getApiJwt(HttpInterface httpInterface) throws IOException {
        try (CloseableHttpResponse response = httpInterface.execute(new HttpGet("https://vimeo.com/_next/viewer"))) {
            HttpClientTools.assertSuccessWithContent(response, "fetch jwt");
            JsonBrowser json = JsonBrowser.parse(response.getEntity().getContent());
            return json.get("jwt").text();
        }
    }

    public static class PlaybackFormat {
        public final String url;
        public final boolean isHls;

        public PlaybackFormat(String url, boolean isHls) {
            this.url = url;
            this.isHls = isHls;
        }
    }
}
