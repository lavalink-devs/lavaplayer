package com.sedmelluq.discord.lavaplayer.source.yamusic;

import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;

import java.util.function.Function;

public interface YandexMusicPlaylistLoader extends YandexMusicApiLoader {
    AudioItem loadPlaylist(String login, String id, String trackProperty, Function<AudioTrackInfo, AudioTrack> trackFactory);

    AudioItem loadPlaylist(String album, String trackProperty, Function<AudioTrackInfo, AudioTrack> trackFactory);
}
