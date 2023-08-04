package com.sedmelluq.discord.lavaplayer.source.yamusic;

import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;

import java.util.function.Function;

public class DefaultYandexMusicTrackLoader extends AbstractYandexMusicApiLoader implements YandexMusicTrackLoader {

    private static final String TRACKS_INFO_FORMAT = "https://api.music.yandex.net/tracks?trackIds=";

    @Override
    public AudioItem loadTrack(String albumId, String trackId, Function<AudioTrackInfo, AudioTrack> trackFactory) {
        StringBuilder id = new StringBuilder(trackId);
        if (!albumId.isEmpty()) id.append(":").append(albumId);

        return extractFromApi(TRACKS_INFO_FORMAT + id, (httpClient, result) -> {
            JsonBrowser entry = result.index(0);
            if (DefaultYandexMusicPlaylistLoader.hasError(entry)) return AudioReference.NO_TRACK;
            return YandexMusicUtils.extractTrack(entry, trackFactory);
        });
    }
}
