package com.sedmelluq.discord.lavaplayer.source.soundcloud;

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterfaceManager;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;

public class DefaultSoundCloudPlaylistLoader implements SoundCloudPlaylistLoader {
    private static final Logger log = LoggerFactory.getLogger(DefaultSoundCloudPlaylistLoader.class);

    protected static final String PLAYLIST_URL_REGEX = "^(?:http://|https://|)(?:www\\.|)(?:m\\.|)soundcloud\\.com/([a-zA-Z0-9-_:]+)/sets/([a-zA-Z0-9-_:]+)/?([a-zA-Z0-9-_:]+)?(?:\\?.*|)$";
    protected static final Pattern playlistUrlPattern = Pattern.compile(PLAYLIST_URL_REGEX);

    protected final SoundCloudDataLoader dataLoader;
    protected final SoundCloudDataReader dataReader;
    protected final SoundCloudFormatHandler formatHandler;

    public DefaultSoundCloudPlaylistLoader(
        SoundCloudDataLoader dataLoader,
        SoundCloudDataReader dataReader,
        SoundCloudFormatHandler formatHandler
    ) {
        this.dataLoader = dataLoader;
        this.dataReader = dataReader;
        this.formatHandler = formatHandler;
    }

    @Override
    public AudioPlaylist load(
        String identifier,
        HttpInterfaceManager httpInterfaceManager,
        Function<AudioTrackInfo, AudioTrack> trackFactory
    ) {
        String url = SoundCloudHelper.nonMobileUrl(identifier);

        if (playlistUrlPattern.matcher(url).matches()) {
            return loadFromSet(httpInterfaceManager, url, trackFactory);
        } else {
            return null;
        }
    }

    protected AudioPlaylist loadFromSet(
        HttpInterfaceManager httpInterfaceManager,
        String playlistWebUrl,
        Function<AudioTrackInfo, AudioTrack> trackFactory
    ) {
        try (HttpInterface httpInterface = httpInterfaceManager.getInterface()) {
            JsonBrowser rootData = dataLoader.load(httpInterface, playlistWebUrl);
            String kind = rootData.get("kind").text();
            JsonBrowser playlistData = dataReader.findPlaylistData(rootData, kind);

            return new BasicAudioPlaylist(
                dataReader.readPlaylistName(playlistData),
                loadPlaylistTracks(httpInterface, playlistData, trackFactory),
                null,
                false
            );
        } catch (IOException e) {
            throw new FriendlyException("Loading playlist from SoundCloud failed.", SUSPICIOUS, e);
        }
    }

    protected List<AudioTrack> loadPlaylistTracks(
        HttpInterface httpInterface,
        JsonBrowser playlistData,
        Function<AudioTrackInfo, AudioTrack> trackFactory
    ) throws IOException {
        String playlistId = dataReader.readPlaylistIdentifier(playlistData);

        List<String> trackIds = dataReader.readPlaylistTracks(playlistData).stream()
            .map(dataReader::readTrackId)
            .collect(Collectors.toList());

        int numTrackIds = trackIds.size();
        List<JsonBrowser> trackDataList = new ArrayList<>();

        for (int i = 0; i < numTrackIds; i += 50) {
            int last = Math.min(i + 50, numTrackIds);
            List<String> trackIdSegment = trackIds.subList(i, last);

            try (CloseableHttpResponse response = httpInterface.execute(new HttpGet(buildTrackListUrl(trackIdSegment)))) {
                HttpClientTools.assertSuccessWithContent(response, "track list response");

                JsonBrowser trackList = JsonBrowser.parse(response.getEntity().getContent());
                trackDataList.addAll(trackList.values());
            }
        }

        sortPlaylistTracks(trackDataList, trackIds);

        int blockedCount = 0;
        List<AudioTrack> tracks = new ArrayList<>();

        for (JsonBrowser trackData : trackDataList) {
            if (dataReader.isTrackBlocked(trackData)) {
                blockedCount++;
            } else {
                try {
                    tracks.add(trackFactory.apply(dataReader.readTrackInfo(
                        trackData,
                        formatHandler.buildFormatIdentifier(
                            formatHandler.chooseBestFormat(dataReader.readTrackFormats(trackData))
                        )
                    )));
                } catch (Exception e) {
                    log.error("In soundcloud playlist {}, failed to load track", playlistId, e);
                }
            }
        }

        if (blockedCount > 0) {
            log.debug("In soundcloud playlist {}, {} tracks were omitted because they are blocked.",
                playlistId, blockedCount);
        }

        return tracks;
    }

    protected URI buildTrackListUrl(List<String> trackIds) {
        try {
            StringJoiner joiner = new StringJoiner(",");
            for (String trackId : trackIds) {
                joiner.add(trackId);
            }

            return new URIBuilder("https://api-v2.soundcloud.com/tracks")
                .addParameter("ids", joiner.toString())
                .build();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    protected void sortPlaylistTracks(List<JsonBrowser> trackDataList, List<String> trackIds) {
        Map<String, Integer> positions = new HashMap<>();

        for (int i = 0; i < trackIds.size(); i++) {
            positions.put(trackIds.get(i), i);
        }

        trackDataList.sort(Comparator.comparingInt(trackData ->
            positions.getOrDefault(dataReader.readTrackId(trackData), Integer.MAX_VALUE)
        ));
    }
}
