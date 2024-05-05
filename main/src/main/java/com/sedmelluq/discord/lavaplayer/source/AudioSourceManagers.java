package com.sedmelluq.discord.lavaplayer.source;

import com.sedmelluq.discord.lavaplayer.container.MediaContainerRegistry;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.beam.BeamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.getyarn.GetyarnAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.local.LocalAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.nico.NicoAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.yamusic.YandexMusicAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;

import java.util.Set;

/**
 * A helper class for registering built-in source managers to a player manager.
 */
public class AudioSourceManagers {
    /**
     * See {@link #registerRemoteSources(AudioPlayerManager, MediaContainerRegistry)}, but with default containers.
     */
    public static void registerRemoteSources(AudioPlayerManager playerManager) {
        registerRemoteSources(playerManager, MediaContainerRegistry.DEFAULT_REGISTRY);
    }

    /**
     * Registers all built-in remote audio sources to the specified player manager. Local file audio source must be
     * registered separately.
     *
     * @param playerManager     Player manager to register the source managers to
     * @param containerRegistry Media container registry to be used by any probing sources.
     */
    public static void registerRemoteSources(AudioPlayerManager playerManager, MediaContainerRegistry containerRegistry) {
        playerManager.registerSourceManager(new YoutubeAudioSourceManager(true, null, null));
        playerManager.registerSourceManager(new YandexMusicAudioSourceManager(true));
        playerManager.registerSourceManager(SoundCloudAudioSourceManager.createDefault());
        playerManager.registerSourceManager(new BandcampAudioSourceManager());
        playerManager.registerSourceManager(new VimeoAudioSourceManager());
        playerManager.registerSourceManager(new TwitchStreamAudioSourceManager());
        playerManager.registerSourceManager(new BeamAudioSourceManager());
        playerManager.registerSourceManager(new GetyarnAudioSourceManager());
        playerManager.registerSourceManager(new NicoAudioSourceManager());
        playerManager.registerSourceManager(new HttpAudioSourceManager(containerRegistry));
    }

    /**
     * Registers all built-in remote audio sources to the specified player manager, excluding the specified sources.
     * Local file audio source must be registered separately.
     *
     * @param playerManager   Player manager to register the source managers to
     * @param excludedSources Source managers to exclude from registration
     */
    @SafeVarargs
    public static void registerRemoteSources(AudioPlayerManager playerManager, Class<? extends AudioSourceManager>... excludedSources) {
        registerRemoteSources(playerManager, MediaContainerRegistry.DEFAULT_REGISTRY, excludedSources);
    }

    /**
     * Registers all built-in remote audio sources to the specified player manager, excluding the specified sources.
     * Local file audio source must be registered separately.
     *
     * @param playerManager     Player manager to register the source managers to
     * @param containerRegistry Media container registry to be used by any probing sources.
     * @param excludedSources   Source managers to exclude from registration
     */
    @SafeVarargs
    public static void registerRemoteSources(AudioPlayerManager playerManager, MediaContainerRegistry containerRegistry, Class<? extends AudioSourceManager>... excludedSources) {
        var excluded = Set.of(excludedSources);
        if (!excluded.contains(YoutubeAudioSourceManager.class)) {
            playerManager.registerSourceManager(new YoutubeAudioSourceManager(true, null, null));
        }
        if (!excluded.contains(YandexMusicAudioSourceManager.class)) {
            playerManager.registerSourceManager(new YandexMusicAudioSourceManager(true));
        }
        if (!excluded.contains(SoundCloudAudioSourceManager.class)) {
            playerManager.registerSourceManager(SoundCloudAudioSourceManager.createDefault());
        }
        if (!excluded.contains(BandcampAudioSourceManager.class)) {
            playerManager.registerSourceManager(new BandcampAudioSourceManager());
        }
        if (!excluded.contains(VimeoAudioSourceManager.class)) {
            playerManager.registerSourceManager(new VimeoAudioSourceManager());
        }
        if (!excluded.contains(TwitchStreamAudioSourceManager.class)) {
            playerManager.registerSourceManager(new TwitchStreamAudioSourceManager());
        }
        if (!excluded.contains(BeamAudioSourceManager.class)) {
            playerManager.registerSourceManager(new BeamAudioSourceManager());
        }
        if (!excluded.contains(GetyarnAudioSourceManager.class)) {
            playerManager.registerSourceManager(new GetyarnAudioSourceManager());
        }
        if (!excluded.contains(NicoAudioSourceManager.class)) {
            playerManager.registerSourceManager(new NicoAudioSourceManager());
        }
        if (!excluded.contains(HttpAudioSourceManager.class)) {
            playerManager.registerSourceManager(new HttpAudioSourceManager(containerRegistry));
        }
    }

    /**
     * Registers the local file source manager to the specified player manager.
     *
     * @param playerManager Player manager to register the source manager to
     */
    public static void registerLocalSource(AudioPlayerManager playerManager) {
        registerLocalSource(playerManager, MediaContainerRegistry.DEFAULT_REGISTRY);
    }

    /**
     * Registers the local file source manager to the specified player manager.
     *
     * @param playerManager     Player manager to register the source manager to
     * @param containerRegistry Media container registry to be used by the local source.
     */
    public static void registerLocalSource(AudioPlayerManager playerManager, MediaContainerRegistry containerRegistry) {
        playerManager.registerSourceManager(new LocalAudioSourceManager(containerRegistry));
    }
}
