package com.sedmelluq.discord.lavaplayer.source.blerp;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.io.*;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.info.AudioTrackInfoBuilder;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;

/**
 * Audio source manager which detects blerp.com tracks by URL.
 */
public class BlerpAudioSourceManager implements HttpConfigurable, AudioSourceManager {
    private static final Pattern BLERP_REGEX = Pattern.compile("https:\\/\\/blerp\\.com\\/soundbites\\/[a-zA-Z0-9]+");
    private static final Pattern BLERP_CDN_REGEX = Pattern.compile("https:\\/\\/cdn\\.blerp\\.com\\/normalized\\/[a-zA-Z0-9]+");

    private final HttpInterfaceManager httpInterfaceManager;

    public BlerpAudioSourceManager() {
        httpInterfaceManager = new ThreadLocalHttpInterfaceManager(
            HttpClientTools
                .createSharedCookiesHttpBuilder()
                .setRedirectStrategy(new HttpClientTools.NoRedirectsStrategy()),
            HttpClientTools.DEFAULT_REQUEST_CONFIG
        );
    }

    @Override
    public String getSourceName() {
        return "blerp.com";
    }

    @Override
    public AudioItem loadItem(AudioPlayerManager manager, AudioReference reference) {
        final Matcher m = BLERP_REGEX.matcher(reference.identifier);

        if (!m.matches()) {
            return null;
        }

        return extractUrlFromPage(reference);
    }

    private AudioTrack createTrack(AudioTrackInfo trackInfo) {
        return new BlerpAudioTrack(trackInfo, this);
    }

    @Override
    public boolean isTrackEncodable(AudioTrack track) {
        return true;
    }

    @Override
    public void encodeTrack(AudioTrack track, DataOutput output) {
        // No custom values that need saving
    }

    @Override
    public AudioTrack decodeTrack(AudioTrackInfo trackInfo, DataInput input) {
        return new BlerpAudioTrack(trackInfo, this);
    }

    @Override
    public void shutdown() {
        // Nothing to shut down
    }

    public HttpInterface getHttpInterface() {
        return httpInterfaceManager.getInterface();
    }

    @Override
    public void configureRequests(Function<RequestConfig, RequestConfig> configurator) {
        httpInterfaceManager.configureRequests(configurator);
    }

    @Override
    public void configureBuilder(Consumer<HttpClientBuilder> configurator) {
        httpInterfaceManager.configureBuilder(configurator);
    }

    private AudioTrack extractUrlFromPage(AudioReference reference) {
        try (final CloseableHttpResponse response = getHttpInterface().execute(new HttpGet(reference.identifier))) {
            final String html = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);

            final Matcher m = BLERP_CDN_REGEX.matcher(html);

            if (m.matches()) {
                final Document document = Jsoup.parse(html);
                final AudioTrackInfo trackInfo = AudioTrackInfoBuilder.empty()
                    .setUri(reference.identifier)
                    .setAuthor("Unknown")
                    .setIsStream(false)
                    .setIdentifier(m.group(1))
                    .setArtworkUrl(document.selectFirst("meta[property=og:image]").attr("content"))
                    .setTitle(document.selectFirst("meta[property=og:title]").attr("content"))
                    .build();

                return createTrack(trackInfo);
            } else {
                throw new FriendlyException("Failed to load info for blerp audio", SUSPICIOUS, null);
            }
        } catch (IOException e) {
            throw new FriendlyException("Failed to load info for blerp audio", SUSPICIOUS, null);
        }
    }
}
