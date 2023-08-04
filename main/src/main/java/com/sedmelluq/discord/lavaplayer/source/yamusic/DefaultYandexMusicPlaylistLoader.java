package com.sedmelluq.discord.lavaplayer.source.yamusic;

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.FutureTools;
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.track.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Function;

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;

public class DefaultYandexMusicPlaylistLoader extends DefaultYandexMusicTrackLoader implements YandexMusicPlaylistLoader {
    private static final String PLAYLIST_INFO_FORMAT = "https://api.music.yandex.net/users/%s/playlists/%s";
    private static final String ALBUM_INFO_FORMAT = "https://api.music.yandex.net/albums/%s/with-tracks";
    private static final String ARTIST_INFO_FORMAT = "https://api.music.yandex.net/artists/%s/brief-info";

    private final ExecutorService tracksLoader;

    public DefaultYandexMusicPlaylistLoader() {
        tracksLoader = Executors.newCachedThreadPool();
    }

    @Override
    public AudioItem loadPlaylist(String login, String id, String trackProperty, Function<AudioTrackInfo, AudioTrack> trackFactory) {
        return loadPlaylistUrl(String.format(PLAYLIST_INFO_FORMAT, login, id), trackProperty, trackFactory);
    }

    @Override
    public AudioItem loadPlaylist(String id, String trackProperty, Function<AudioTrackInfo, AudioTrack> trackFactory) {
        if (trackProperty.equals("volumes")) {
            return loadPlaylistUrl(String.format(ALBUM_INFO_FORMAT, id), trackProperty, trackFactory);
        } else {
            return loadPlaylistUrl(String.format(ARTIST_INFO_FORMAT, id), trackProperty, trackFactory);
        }
    }

    private AudioItem loadPlaylistUrl(String url, String trackProperty, Function<AudioTrackInfo, AudioTrack> trackFactory) {
        return extractFromApi(url, (httpClient, result) -> {
            if (hasError(result)) return AudioReference.NO_TRACK;
            JsonBrowser volumes = result.get(trackProperty);
            if (volumes.isNull()) {
                throw new FriendlyException("Volumes is empty", SUSPICIOUS, null);
            }

            List<Future<AudioTrack>> futures = new ArrayList<>();
            CompletionService<AudioTrack> completionService = new ExecutorCompletionService<>(tracksLoader);
            for (JsonBrowser trackInfo : volumes.values()) {
                if (trackInfo.isList()) {
                    for (JsonBrowser innerInfo : trackInfo.values()) {
                        futures.add(completionService.submit(() -> loadTrack(innerInfo, trackFactory)));
                    }
                } else {
                    futures.add(completionService.submit(() -> loadTrack(trackInfo, trackFactory)));
                }
            }
            if (futures.isEmpty()) {
                return AudioReference.NO_TRACK;
            }

            List<AudioTrack> tracks = FutureTools.awaitList(completionService, futures);
            if (futures.isEmpty()) {
                return AudioReference.NO_TRACK;
            }

            String name;
            if (trackProperty.equals("volumes") || trackProperty.equals("tracks")) {
                name = result.get("title").text();
            } else {
                name = result.get("artist").get("name").text();
            }

            return new BasicAudioPlaylist(name, tracks, null, false);
        });
    }

    static boolean hasError(JsonBrowser result) {
        JsonBrowser error = result.get("error");
        if (!error.isNull()) {
            String code = error.text();
            if ("not-found".equals(code)) {
                return true;
            }
            throw new FriendlyException(String.format("Yandex Music returned an error code: %s", code), SUSPICIOUS, null);
        }
        return false;
    }

    private AudioTrack loadTrack(JsonBrowser trackInfo, Function<AudioTrackInfo, AudioTrack> trackFactory) {
        if (!trackInfo.get("title").isNull()) {
            return YandexMusicUtils.extractTrack(trackInfo, trackFactory);
        }
        if (!trackInfo.get("track").isNull()) {
            return YandexMusicUtils.extractTrack(trackInfo, trackFactory);
        }
        String trackId = trackInfo.get("id").text();
        String albumId = trackInfo.get("albumId").text();
        if (trackId == null || albumId == null) {
            throw new FriendlyException("Could not load playlist track", FriendlyException.Severity.COMMON, null);
        }
        return (AudioTrack) loadTrack(albumId, trackId, trackFactory);
    }

    @Override
    public void shutdown() {
        super.shutdown();
        tracksLoader.shutdown();
    }
}
