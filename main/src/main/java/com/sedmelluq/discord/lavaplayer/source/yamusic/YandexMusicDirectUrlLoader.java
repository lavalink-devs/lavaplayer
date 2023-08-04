package com.sedmelluq.discord.lavaplayer.source.yamusic;

public interface YandexMusicDirectUrlLoader extends YandexMusicApiLoader {
    String getDirectUrl(String trackId, String codec);
}
