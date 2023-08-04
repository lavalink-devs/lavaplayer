package com.sedmelluq.discord.lavaplayer.source.yamusic;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.ExceptionTools;
import com.sedmelluq.discord.lavaplayer.tools.http.ExtendedHttpConfigurable;
import com.sedmelluq.discord.lavaplayer.tools.http.MultiHttpConfigurable;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpConfigurable;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterfaceManager;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Audio source manager that implements finding Yandex Music tracks based on URL.
 */
public class YandexMusicAudioSourceManager implements AudioSourceManager, HttpConfigurable {
    private static final String PROTOCOL_REGEX = "https?://";
    private static final String DOMAIN_REGEX = "music\\.yandex\\.[a-zA-Z]+";
    private static final String TRACK_ID_REGEX = "track/([0-9]+)(?:\\?.*|)";
    private static final String ALBUM_ID_REGEX = "album/([0-9]+)(?:\\?.*|)";
    private static final String ARTIST_ID_REGEX = "artist/([0-9]+)(?:/tracks)?(?:\\?.*|)";
    private static final String PLAYLIST_ID_REGEX = "playlists/([0-9]+)(?:\\?.*|)";
    private static final String USER_REGEX = "users/(.+)";

    private static final Pattern trackUrlPattern = Pattern.compile("^" +
        PROTOCOL_REGEX + DOMAIN_REGEX + "/" +
        ALBUM_ID_REGEX + "/" + TRACK_ID_REGEX + "$"
    );
    private static final Pattern shortTrackUrlPattern = Pattern.compile("^" +
        PROTOCOL_REGEX + DOMAIN_REGEX + "/" +
        TRACK_ID_REGEX + "$"
    );
    private static final Pattern albumUrlPattern = Pattern.compile("^" +
        PROTOCOL_REGEX + DOMAIN_REGEX + "/" +
        ALBUM_ID_REGEX + "$"
    );
    private static final Pattern artistUrlPattern = Pattern.compile("^" +
        PROTOCOL_REGEX + DOMAIN_REGEX + "/" +
        ARTIST_ID_REGEX + "$"
    );
    private static final Pattern playlistUrlPattern = Pattern.compile("^" +
        PROTOCOL_REGEX + DOMAIN_REGEX + "/" +
        USER_REGEX + "/" + PLAYLIST_ID_REGEX + "$"
    );

    private final boolean allowSearch;

    private final HttpInterfaceManager httpInterfaceManager;
    private final ExtendedHttpConfigurable combinedHttpConfiguration;

    private final YandexMusicDirectUrlLoader directUrlLoader;
    private final YandexMusicTrackLoader trackLoader;
    private final YandexMusicPlaylistLoader playlistLoader;
    private final YandexMusicSearchResultLoader searchResultLoader;

    public YandexMusicAudioSourceManager() {
        this(true);
    }

    public YandexMusicAudioSourceManager(boolean allowSearch) {
        this(
            allowSearch,
            new DefaultYandexMusicTrackLoader(),
            new DefaultYandexMusicPlaylistLoader(),
            new DefaultYandexMusicDirectUrlLoader(),
            new DefaultYandexSearchProvider()
        );
    }

    /**
     * Create an instance.
     */
    public YandexMusicAudioSourceManager(
        boolean allowSearch,
        YandexMusicTrackLoader trackLoader,
        YandexMusicPlaylistLoader playlistLoader,
        YandexMusicDirectUrlLoader directUrlLoader,
        YandexMusicSearchResultLoader searchResultLoader) {
        this.allowSearch = allowSearch;
        this.trackLoader = trackLoader;
        this.playlistLoader = playlistLoader;
        this.directUrlLoader = directUrlLoader;
        this.searchResultLoader = searchResultLoader;

        httpInterfaceManager = HttpClientTools.createDefaultThreadLocalManager();
        httpInterfaceManager.setHttpContextFilter(new YandexHttpContextFilter());

        combinedHttpConfiguration = new MultiHttpConfigurable(Arrays.asList(
            httpInterfaceManager,
            trackLoader.getHttpConfiguration(),
            playlistLoader.getHttpConfiguration(),
            directUrlLoader.getHttpConfiguration(),
            searchResultLoader.getHttpConfiguration()
        ));
    }

    @Override
    public AudioItem loadItem(AudioPlayerManager manager, AudioReference reference) {
        Matcher matcher;
        if ((matcher = trackUrlPattern.matcher(reference.identifier)).matches()) {
            return trackLoader.loadTrack(matcher.group(1), matcher.group(2), this::getTrack);
        }
        if ((matcher = shortTrackUrlPattern.matcher(reference.identifier)).matches()) {
            return trackLoader.loadTrack("", matcher.group(1), this::getTrack);
        }
        if ((matcher = playlistUrlPattern.matcher(reference.identifier)).matches()) {
            return playlistLoader.loadPlaylist(matcher.group(1), matcher.group(2), "tracks", this::getTrack);
        }
        if ((matcher = albumUrlPattern.matcher(reference.identifier)).matches()) {
            return playlistLoader.loadPlaylist(matcher.group(1), "volumes", this::getTrack);
        }
        if ((matcher = artistUrlPattern.matcher(reference.identifier)).matches()) {
            return playlistLoader.loadPlaylist(matcher.group(1), "popularTracks", this::getTrack);
        }
        if (allowSearch) {
            return searchResultLoader.loadSearchResult(reference.identifier, playlistLoader, this::getTrack);
        }
        return null;
    }

    @Override
    public boolean isTrackEncodable(AudioTrack track) {
        return true;
    }

    @Override
    public void encodeTrack(AudioTrack track, DataOutput output) throws IOException {
        // No special values to encode
    }

    @Override
    public AudioTrack decodeTrack(AudioTrackInfo trackInfo, DataInput input) throws IOException {
        return new YandexMusicAudioTrack(trackInfo, this);
    }

    public AudioTrack getTrack(AudioTrackInfo info) {
        return new YandexMusicAudioTrack(info, this);
    }

    @Override
    public void shutdown() {
        ExceptionTools.closeWithWarnings(httpInterfaceManager);
        trackLoader.shutdown();
        playlistLoader.shutdown();
        searchResultLoader.shutdown();
        directUrlLoader.shutdown();
    }

    public YandexMusicDirectUrlLoader getDirectUrlLoader() {
        return directUrlLoader;
    }

    /**
     * @return Get an HTTP interface for a playing track.
     */
    public HttpInterface getHttpInterface() {
        return httpInterfaceManager.getInterface();
    }

    @Override
    public void configureRequests(Function<RequestConfig, RequestConfig> configurator) {
        combinedHttpConfiguration.configureRequests(configurator);
    }

    @Override
    public void configureBuilder(Consumer<HttpClientBuilder> configurator) {
        combinedHttpConfiguration.configureBuilder(configurator);
    }

    public ExtendedHttpConfigurable getHttpConfiguration() {
        return combinedHttpConfiguration;
    }

    public ExtendedHttpConfigurable getMainHttpConfiguration() {
        return httpInterfaceManager;
    }

    public ExtendedHttpConfigurable getTrackLHttpConfiguration() {
        return trackLoader.getHttpConfiguration();
    }

    public ExtendedHttpConfigurable getPlaylistLHttpConfiguration() {
        return playlistLoader.getHttpConfiguration();
    }

    public ExtendedHttpConfigurable getDirectUrlLHttpConfiguration() {
        return directUrlLoader.getHttpConfiguration();
    }

    public ExtendedHttpConfigurable getSearchHttpConfiguration() {
        return searchResultLoader.getHttpConfiguration();
    }

    @Override
    public String getSourceName() {
        return "yandex-music";
    }
}
