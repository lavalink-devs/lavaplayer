package com.sedmelluq.discord.lavaplayer.source.vimeo;

import com.sedmelluq.discord.lavaplayer.container.mpeg.MpegAudioTrack;
import com.sedmelluq.discord.lavaplayer.container.playlists.ExtendedM3uParser;
import com.sedmelluq.discord.lavaplayer.container.playlists.HlsStreamTrack;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.tools.io.PersistentHttpStream;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.DelegatedAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;

/**
 * Audio track that handles processing Vimeo tracks.
 */
public class VimeoAudioTrack extends DelegatedAudioTrack {
    private static final Logger log = LoggerFactory.getLogger(VimeoAudioTrack.class);

    private final VimeoAudioSourceManager sourceManager;

    /**
     * @param trackInfo     Track info
     * @param sourceManager Source manager which was used to find this track
     */
    public VimeoAudioTrack(AudioTrackInfo trackInfo, VimeoAudioSourceManager sourceManager) {
        super(trackInfo);

        this.sourceManager = sourceManager;
    }

    @Override
    public void process(LocalAudioTrackExecutor localExecutor) throws Exception {
        try (HttpInterface httpInterface = sourceManager.getHttpInterface()) {
            PlaybackSource playbackSource = getPlaybackSource(httpInterface);

            log.debug("Starting Vimeo track. HLS: {}, URL: {}", playbackSource.isHls, playbackSource.url);

            if (playbackSource.isHls) {
                processDelegate(new HlsStreamTrack(
                    trackInfo,
                    extractHlsAudioPlaylistUrl(httpInterface, playbackSource.url),
                    sourceManager.getHttpInterfaceManager(),
                    true
                ), localExecutor);
            } else {
                try (PersistentHttpStream stream = new PersistentHttpStream(httpInterface, new URI(playbackSource.url), null)) {
                    processDelegate(new MpegAudioTrack(trackInfo, stream), localExecutor);
                }
            }
        }
    }

    private PlaybackSource getPlaybackSource(HttpInterface httpInterface) throws IOException {
        JsonBrowser config = loadPlayerConfig(httpInterface);
        if (config == null) {
            throw new FriendlyException("Track information not present on the page.", SUSPICIOUS, null);
        }

        String trackConfigUrl = config.get("player").get("config_url").text();
        JsonBrowser trackConfig = loadTrackConfig(httpInterface, trackConfigUrl);
        JsonBrowser files = trackConfig.get("request").get("files");

        if (!files.get("progressive").values().isEmpty()) {
            String url = files.get("progressive").index(0).get("url").text();
            return new PlaybackSource(url, false);
        } else {
            JsonBrowser hls = files.get("hls");
            String defaultCdn = hls.get("default_cdn").text();
            return new PlaybackSource(hls.get("cdns").get(defaultCdn).get("url").text(), true);
        }
    }

    private static class PlaybackSource {
        public String url;
        public boolean isHls;

        public PlaybackSource(String url, boolean isHls) {
            this.url = url;
            this.isHls = isHls;
        }
    }

    private JsonBrowser loadPlayerConfig(HttpInterface httpInterface) throws IOException {
        try (CloseableHttpResponse response = httpInterface.execute(new HttpGet(trackInfo.identifier))) {
            int statusCode = response.getStatusLine().getStatusCode();

            if (!HttpClientTools.isSuccessWithContent(statusCode)) {
                throw new FriendlyException("Server responded with an error.", SUSPICIOUS,
                    new IllegalStateException("Response code for player config is " + statusCode));
            }

            return sourceManager.loadConfigJsonFromPageContent(IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8));
        }
    }

    private JsonBrowser loadTrackConfig(HttpInterface httpInterface, String trackAccessInfoUrl) throws IOException {
        try (CloseableHttpResponse response = httpInterface.execute(new HttpGet(trackAccessInfoUrl))) {
            int statusCode = response.getStatusLine().getStatusCode();

            if (!HttpClientTools.isSuccessWithContent(statusCode)) {
                throw new FriendlyException("Server responded with an error.", SUSPICIOUS,
                    new IllegalStateException("Response code for track access info is " + statusCode));
            }

            return JsonBrowser.parse(response.getEntity().getContent());
        }
    }

    protected String resolveRelativeUrl(String baseUrl, String url) {
        int upTraversals = 0;
        while (url.startsWith("../")) {
            upTraversals++;
            url = url.substring(3);
        }

        for (int i = 0; i < upTraversals; i++) {
            baseUrl = baseUrl.substring(0, baseUrl.lastIndexOf('/'));
        }

        return baseUrl + ((url.startsWith("/")) ? url : "/" + url);
    }

    /** Vimeo HLS uses separate audio and video. This extracts the audio playlist URL from EXT-X-MEDIA */
    private String extractHlsAudioPlaylistUrl(HttpInterface httpInterface, String videoPlaylistUrl) throws IOException {
        String url = null;
        try (CloseableHttpResponse response = httpInterface.execute(new HttpGet(videoPlaylistUrl))) {
            int statusCode = response.getStatusLine().getStatusCode();

            if (!HttpClientTools.isSuccessWithContent(statusCode)) {
                throw new FriendlyException("Server responded with an error.", SUSPICIOUS,
                    new IllegalStateException("Response code for track access info is " + statusCode));
            }

            String bodyString = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            for (String rawLine : bodyString.split("\n")) {
                ExtendedM3uParser.Line line = ExtendedM3uParser.parseLine(rawLine);
                if (Objects.equals(line.directiveName, "EXT-X-MEDIA")
                    && Objects.equals(line.directiveArguments.get("TYPE"), "AUDIO")) {
                    url = line.directiveArguments.get("URI");
                    break;
                }
            }
        }

        if (url == null) throw new FriendlyException("Failed to find audio playlist URL.", SUSPICIOUS,
            new IllegalStateException("Valid audio directive was not found"));

        return resolveRelativeUrl(videoPlaylistUrl.substring(0, videoPlaylistUrl.lastIndexOf('/')), url);
    }

    @Override
    protected AudioTrack makeShallowClone() {
        return new VimeoAudioTrack(trackInfo, sourceManager);
    }

    @Override
    public AudioSourceManager getSourceManager() {
        return sourceManager;
    }
}
