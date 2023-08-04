package com.sedmelluq.discord.lavaplayer.source.yamusic;

import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;

import java.util.function.Function;

public interface YandexMusicSearchResultLoader extends YandexMusicApiLoader {
    AudioItem loadSearchResult(String query,
                               YandexMusicPlaylistLoader playlistLoader,
                               Function<AudioTrackInfo, AudioTrack> trackFactory);
}
