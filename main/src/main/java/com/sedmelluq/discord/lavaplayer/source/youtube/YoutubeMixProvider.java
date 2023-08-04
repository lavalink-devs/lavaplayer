package com.sedmelluq.discord.lavaplayer.source.youtube;

import com.sedmelluq.discord.lavaplayer.tools.DataFormatTools;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.tools.ThumbnailTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeConstants.NEXT_URL;
import static com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeConstants.WATCH_URL_PREFIX;
import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;

/**
 * Handles loading of YouTube mixes.
 */
public class YoutubeMixProvider implements YoutubeMixLoader {
    /**
     * Loads tracks from mix in parallel into a playlist entry.
     *
     * @param mixId           ID of the mix
     * @param selectedVideoId Selected track, {@link AudioPlaylist#getSelectedTrack()} will return this.
     * @return Playlist of the tracks in the mix.
     */
    public AudioPlaylist load(
        HttpInterface httpInterface,
        String mixId,
        String selectedVideoId,
        Function<AudioTrackInfo, AudioTrack> trackFactory
    ) {
        String playlistTitle = "YouTube mix";
        List<AudioTrack> tracks = new ArrayList<>();

        HttpPost post = new HttpPost(NEXT_URL);
        YoutubeClientConfig clientConfig = YoutubeClientConfig.ANDROID.copy()
            .withRootField("videoId", selectedVideoId)
            .withRootField("playlistId", mixId)
            .setAttribute(httpInterface);
        StringEntity payload = new StringEntity(clientConfig.toJsonString(), "UTF-8");
        post.setEntity(payload);
        try (CloseableHttpResponse response = httpInterface.execute(post)) {
            HttpClientTools.assertSuccessWithContent(response, "mix response");

            JsonBrowser body = JsonBrowser.parse(response.getEntity().getContent());
            JsonBrowser playlist = body.get("contents")
                .get("singleColumnWatchNextResults")
                .get("playlist")
                .get("playlist");

            JsonBrowser title = playlist.get("title");

            if (!title.isNull()) {
                playlistTitle = title.text();
            }

            extractPlaylistTracks(playlist.get("contents"), tracks, trackFactory);
        } catch (IOException e) {
            throw new FriendlyException("Could not read mix page.", SUSPICIOUS, e);
        }

        if (tracks.isEmpty()) {
            throw new FriendlyException("Could not find tracks from mix.", SUSPICIOUS, null);
        }

        AudioTrack selectedTrack = findSelectedTrack(tracks, selectedVideoId);
        return new BasicAudioPlaylist(playlistTitle, tracks, selectedTrack, false);
    }

    private void extractPlaylistTracks(
        JsonBrowser browser,
        List<AudioTrack> tracks,
        Function<AudioTrackInfo, AudioTrack> trackFactory
    ) {
        for (JsonBrowser video : browser.values()) {
            JsonBrowser renderer = video.get("playlistPanelVideoRenderer");

            if (!renderer.get("unplayableText").isNull()) {
                return;
            }

            String title = renderer.get("title").get("runs").index(0).get("text").text();
            String author = renderer.get("longBylineText").get("runs").index(0).get("text").text();
            String durationStr = renderer.get("lengthText").get("runs").index(0).get("text").text();
            long duration = DataFormatTools.durationTextToMillis(durationStr);
            String identifier = renderer.get("videoId").text();
            String uri = WATCH_URL_PREFIX + identifier;

            AudioTrackInfo trackInfo = new AudioTrackInfo(title, author, duration, identifier, false, uri,
                ThumbnailTools.getYouTubeThumbnail(renderer, identifier), null);
            tracks.add(trackFactory.apply(trackInfo));
        }
    }

    private AudioTrack findSelectedTrack(List<AudioTrack> tracks, String selectedVideoId) {
        if (selectedVideoId != null) {
            for (AudioTrack track : tracks) {
                if (selectedVideoId.equals(track.getIdentifier())) {
                    return track;
                }
            }
        }

        return null;
    }
}
