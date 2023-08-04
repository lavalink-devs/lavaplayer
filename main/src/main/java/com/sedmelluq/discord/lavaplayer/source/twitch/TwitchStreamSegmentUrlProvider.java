package com.sedmelluq.discord.lavaplayer.source.twitch;

import com.sedmelluq.discord.lavaplayer.container.playlists.ExtendedM3uParser;
import com.sedmelluq.discord.lavaplayer.source.stream.M3uStreamSegmentUrlProvider;
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * Provider for Twitch segment URLs from a channel.
 */
public class TwitchStreamSegmentUrlProvider extends M3uStreamSegmentUrlProvider {
    private static final String TOKEN_PARAMETER = "token";

    private static final Logger log = LoggerFactory.getLogger(TwitchStreamSegmentUrlProvider.class);

    private final String channelName;
    private final TwitchStreamAudioSourceManager manager;

    private String streamSegmentPlaylistUrl;
    private long tokenExpirationTime;

    /**
     * @param channelName Channel identifier.
     * @param manager     Twitch source manager.
     */
    public TwitchStreamSegmentUrlProvider(String channelName, TwitchStreamAudioSourceManager manager) {
        this.channelName = channelName;
        this.manager = manager;
        this.tokenExpirationTime = -1;
    }

    @Override
    protected String getQualityFromM3uDirective(ExtendedM3uParser.Line directiveLine) {
        return directiveLine.directiveArguments.get("VIDEO");
    }

    @Override
    protected String fetchSegmentPlaylistUrl(HttpInterface httpInterface) throws IOException {
        if (System.currentTimeMillis() < tokenExpirationTime) {
            return streamSegmentPlaylistUrl;
        }

        JsonBrowser tokenJson = manager.fetchAccessToken(channelName);
        AccessToken token = new AccessToken(
            JsonBrowser.parse(tokenJson.get("data").get("streamPlaybackAccessToken").get("value").text()),
            tokenJson.get("data").get("streamPlaybackAccessToken").get("signature").text()
        );
        String url = getChannelStreamsUrl(token).toString();
        HttpUriRequest request = new HttpGet(url);
        ChannelStreams streams = loadChannelStreamsInfo(HttpClientTools.fetchResponseLines(httpInterface, request, "channel streams list"));

        if (streams.entries.isEmpty()) {
            throw new IllegalStateException("No streams available on channel.");
        }

        ChannelStreamInfo stream = streams.entries.get(streams.entries.size() - 1);

        log.debug("Chose stream with quality {} from url {}", stream.quality, stream.url);
        streamSegmentPlaylistUrl = stream.url;

        long tokenServerExpirationTime = token.value.get("expires").as(Long.class) * 1000L;
        tokenExpirationTime = System.currentTimeMillis() + (tokenServerExpirationTime - streams.serverTime) - 5000;

        return streamSegmentPlaylistUrl;
    }

    @Override
    protected HttpUriRequest createSegmentGetRequest(String url) {
        return manager.createGetRequest(url);
    }

    private ChannelStreams loadChannelStreamsInfo(String[] lines) {
        List<ChannelStreamInfo> streams = loadChannelStreamsList(lines);
        ExtendedM3uParser.Line twitchInfoLine = null;

        for (String lineText : lines) {
            ExtendedM3uParser.Line line = ExtendedM3uParser.parseLine(lineText);

            if (line.isDirective() && "EXT-X-TWITCH-INFO".equals(line.directiveName)) {
                twitchInfoLine = line;
            }
        }

        return buildChannelStreamsInfo(twitchInfoLine, streams);
    }

    private ChannelStreams buildChannelStreamsInfo(ExtendedM3uParser.Line twitchInfoLine, List<ChannelStreamInfo> streams) {
        String serverTimeValue = twitchInfoLine != null ? twitchInfoLine.directiveArguments.get("SERVER-TIME") : null;

        if (serverTimeValue == null) {
            throw new IllegalStateException("Required server time information not available.");
        }

        return new ChannelStreams(
            (long) (Double.parseDouble(serverTimeValue) * 1000.0),
            streams
        );
    }

    private URI getChannelStreamsUrl(AccessToken token) {
        try {
            return new URIBuilder("https://usher.ttvnw.net/api/channel/hls/" + channelName + ".m3u8")
                .addParameter(TOKEN_PARAMETER, token.value.format())
                .addParameter("sig", token.signature)
                .addParameter("allow_source", "true")
                .addParameter("allow_spectre", "true")
                .addParameter("allow_audio_only", "true")
                .addParameter("player_backend", "html5")
                .addParameter("expgroup", "regular")
                .build();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static class ChannelStreams {
        private final long serverTime;
        private final List<ChannelStreamInfo> entries;

        private ChannelStreams(long serverTime, List<ChannelStreamInfo> entries) {
            this.serverTime = serverTime;
            this.entries = entries;
        }
    }

    private static class AccessToken {
        private final JsonBrowser value;
        private final String signature;

        private AccessToken(JsonBrowser value, String signature) {
            this.value = value;
            this.signature = signature;
        }
    }
}
