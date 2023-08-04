package com.sedmelluq.discord.lavaplayer.source.yamusic;

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.track.*;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DefaultYandexSearchProvider extends AbstractYandexMusicApiLoader implements YandexMusicSearchResultLoader {

    private static final int DEFAULT_LIMIT = 10;

    private static final String TRACKS_INFO_FORMAT = "https://api.music.yandex.net/search?type=%s&page=0&text=%s";

    private static final String SEARCH_PREFIX = "ymsearch";

    private static final Pattern SEARCH_PATTERN = Pattern.compile("ymsearch(:([a-zA-Z]+))?(:([0-9]+))?:([^:]+)");

    @Override
    public AudioItem loadSearchResult(String query, YandexMusicPlaylistLoader playlistLoader, Function<AudioTrackInfo, AudioTrack> trackFactory) {
        if (query == null || !query.startsWith(SEARCH_PREFIX)) {
            return null;
        }
        Matcher matcher = SEARCH_PATTERN.matcher(query);
        if (!matcher.find()) {
            return null;
        }
        String type = getValidType(matcher.group(2));
        int limit = getValidLimit(matcher.group(4));
        String text = matcher.group(5);
        try {
            return extractFromApi(String.format(TRACKS_INFO_FORMAT, type, URLEncoder.encode(text, "UTF-8")), (httpClient, result) -> {
                if ("track".equalsIgnoreCase(type)) {
                    return loadTracks(getResults(result, "tracks"), limit, trackFactory);
                }
                if ("album".equalsIgnoreCase(type)) {
                    return loadAlbum(getResults(result, "albums"), playlistLoader, trackFactory);
                }
                if ("playlist".equalsIgnoreCase(type)) {
                    return loadPlaylist(getResults(result, "playlists"), playlistLoader, trackFactory);
                }
                return AudioReference.NO_TRACK;
            });
        } catch (Exception e) {
            throw new FriendlyException("Could not load search results", FriendlyException.Severity.SUSPICIOUS, e);
        }
    }

    private AudioItem loadTracks(List<JsonBrowser> results, int limit, Function<AudioTrackInfo, AudioTrack> trackFactory) {
        List<AudioTrack> tracks = new ArrayList<>(limit);
        for (JsonBrowser entry : results) {
            tracks.add(YandexMusicUtils.extractTrack(entry, trackFactory));
            if (tracks.size() >= limit) {
                break;
            }
        }
        if (tracks.isEmpty()) {
            return AudioReference.NO_TRACK;
        }
        return new BasicAudioPlaylist("Yandex search result", tracks, null, true);
    }

    private AudioItem loadPlaylist(List<JsonBrowser> results,
                                   YandexMusicPlaylistLoader playlistLoader,
                                   Function<AudioTrackInfo, AudioTrack> trackFactory) {
        if (results.isEmpty()) {
            return AudioReference.NO_TRACK;
        }
        JsonBrowser first = results.get(0);
        return playlistLoader.loadPlaylist(first.get("owner").get("login").safeText(),
            first.get("kind").safeText(), "tracks", trackFactory);
    }

    private AudioItem loadAlbum(List<JsonBrowser> results,
                                YandexMusicPlaylistLoader playlistLoader,
                                Function<AudioTrackInfo, AudioTrack> trackFactory) {
        if (results.isEmpty()) {
            return AudioReference.NO_TRACK;
        }
        JsonBrowser first = results.get(0);
        return playlistLoader.loadPlaylist(first.get("id").safeText(), "volumes", trackFactory);
    }

    private List<JsonBrowser> getResults(JsonBrowser root, String property) {
        root = root.get(property);
        if (root.isNull()) {
            return Collections.emptyList();
        }
        root = root.get("results");
        if (root.isNull() || !root.isList()) {
            throw new FriendlyException("Invalid search response [2]", FriendlyException.Severity.COMMON, null);
        }
        return root.values();
    }

    private String getValidType(String type) {
        if (type == null) {
            return "track";
        }
        type = type.trim();
        if (type.equalsIgnoreCase("track")
            || type.equalsIgnoreCase("playlist")
            || type.equalsIgnoreCase("album")) {
            return type;
        }
        return "track";
    }

    private Integer getValidLimit(String limit) {
        try {
            if (limit != null) {
                int result = Integer.parseInt(limit);
                if (result > 0 && result < 100) {
                    return result;
                }
            }
        } catch (NumberFormatException e) {
            // fall down to default
        }
        return DEFAULT_LIMIT;
    }
}
