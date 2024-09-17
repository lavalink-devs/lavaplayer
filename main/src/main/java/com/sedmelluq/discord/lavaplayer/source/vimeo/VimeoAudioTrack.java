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
     * @param trackInfo Track info
     * @param sourceManager Source manager which was used to find this track
     */
    public VimeoAudioTrack(AudioTrackInfo trackInfo, VimeoAudioSourceManager sourceManager) {
        super(trackInfo);

        this.sourceManager = sourceManager;
    }

    @Override
    public void process(LocalAudioTrackExecutor localExecutor) throws Exception {
        try (HttpInterface httpInterface = sourceManager.getHttpInterface()) {
            JsonBrowser videoData = sourceManager.getVideoFromApi(httpInterface, trackInfo.identifier);
            VimeoAudioSourceManager.PlaybackFormat playbackFormat = sourceManager.getPlaybackFormat(httpInterface, videoData.get("config_url").text());

            log.debug("Starting Vimeo track. HLS: {}, URL: {}", playbackFormat.isHls, playbackFormat.url);

            if (playbackFormat.isHls) {
                processDelegate(
                    new HlsStreamTrack(trackInfo, extractHlsAudioPlaylistUrl(httpInterface, playbackFormat.url), sourceManager.getHttpInterfaceManager(), true),
                    localExecutor
                );
            } else {
                try (PersistentHttpStream stream = new PersistentHttpStream(httpInterface, new URI(playbackFormat.url), null)) {
                    processDelegate(new MpegAudioTrack(trackInfo, stream), localExecutor);
                }
            }
        }
    }

    protected String resolveRelativeUrl(String baseUrl, String url) {
        while (url.startsWith("../")) {
            url = url.substring(3);
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

                if (Objects.equals(line.directiveName, "EXT-X-MEDIA") && Objects.equals(line.directiveArguments.get("TYPE"), "AUDIO")) {
                    url = line.directiveArguments.get("URI");
                    break;
                }
            }
        }

        if (url == null) {
            throw new FriendlyException("Failed to find audio playlist URL.", SUSPICIOUS,
                new IllegalStateException("Valid audio directive was not found"));
        }

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
