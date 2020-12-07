package com.sedmelluq.discord.lavaplayer.source.yamusic;

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;

import java.util.function.Function;

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;

public class DefaultYandexMusicTrackLoader extends AbstractYandexMusicApiLoader implements YandexMusicTrackLoader {

  private static final String TRACKS_INFO_FORMAT = "https://api.music.yandex.net/tracks?trackIds=";
  private static final String TRACK_URL_FORMAT = "https://music.yandex.ru/album/%s/track/%s";

  @Override
  public AudioItem loadTrack(String albumId, String trackId, Function<AudioTrackInfo, AudioTrack> trackFactory) {
    return extractFromApi(TRACKS_INFO_FORMAT + String.format("%s:%s", trackId, albumId), (httpClient, result) -> {
      JsonBrowser entry = result.index(0);
      JsonBrowser error = entry.get("error");
      if (!error.isNull()) {
        String code = error.text();
        if ("not-found".equals(code)) {
          return AudioReference.NO_TRACK;
        }
        throw new FriendlyException(String.format("Yandex Music returned an error code: %s", code), SUSPICIOUS, null);
      }
      return YandexMusicUtils.extractTrack(entry, trackFactory);
    });
  }
}
