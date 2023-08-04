package com.sedmelluq.discord.lavaplayer.source.yamusic;

import com.sedmelluq.discord.lavaplayer.tools.http.ExtendedHttpConfigurable;

public interface YandexMusicApiLoader {
    ExtendedHttpConfigurable getHttpConfiguration();

    void shutdown();
}
