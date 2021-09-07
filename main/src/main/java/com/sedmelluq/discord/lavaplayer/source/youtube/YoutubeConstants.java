package com.sedmelluq.discord.lavaplayer.source.youtube;

public class YoutubeConstants {

    // YouTube constants
    static final String YOUTUBE_ORIGIN = "https://www.youtube.com";
    static final String BASE_URL = YOUTUBE_ORIGIN + "/youtubei/v1";
    static final String INNERTUBE_API_KEY = "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8";
    static final String CLIENT_NAME = "ANDROID";
    static final String CLIENT_VERSION = "16.24";
    static final String BASE_PAYLOAD = "{\"context\":{\"client\":{\"clientName\":\"" + CLIENT_NAME + "\",\"clientVersion\":\"" + CLIENT_VERSION + "\"}},";

    static final String PLAYER_URL = BASE_URL + "/player?key=" + INNERTUBE_API_KEY;
    static final String PLAYER_PAYLOAD = BASE_PAYLOAD + "\"racyCheckOk\":true,\"contentCheckOk\":true,\"videoId\":\"%s\",\"playbackContext\":{\"contentPlaybackContext\":{\"signatureTimestamp\":%s}}}";
    static final String VERIFY_AGE_URL = BASE_URL + "/verify_age?key=" + INNERTUBE_API_KEY;
    static final String VERIFY_AGE_PAYLOAD = BASE_PAYLOAD + "\"nextEndpoint\":{\"urlEndpoint\":{\"url\":\"%s\"}},\"setControvercy\":true}";
    static final String SEARCH_URL = BASE_URL + "/search?key=" + INNERTUBE_API_KEY;
    static final String SEARCH_PAYLOAD = BASE_PAYLOAD + "\"query\":\"%s\",\"params\":\"EgIQAQ==\"}";
    static final String BROWSE_URL = BASE_URL + "/browse?key=" + INNERTUBE_API_KEY;
    static final String BROWSE_CONTINUATION_PAYLOAD = BASE_PAYLOAD + "\"continuation\":\"%s\"}";
    static final String BROWSE_PLAYLIST_PAYLOAD = BASE_PAYLOAD + "\"browseId\":\"VL%s\"}";
    static final String NEXT_URL = BASE_URL + "/next?key=" + INNERTUBE_API_KEY;
    static final String NEXT_PAYLOAD = BASE_PAYLOAD + "\"videoId\":\"%s\",\"playlistId\":\"%s\"}";

    // YouTube Music constants
    static final String MUSIC_BASE_URL = "https://music.youtube.com/youtubei/v1";
    static final String MUSIC_INNERTUBE_API_KEY = "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX30";
    static final String MUSIC_CLIENT_NAME = "WEB_REMIX";
    static final String MUSIC_CLIENT_VERSION = "0.1";
    static final String MUSIC_BASE_PAYLOAD = "{\"context\":{\"client\":{\"clientName\":\"" + MUSIC_CLIENT_NAME + "\",\"clientVersion\":\"" + MUSIC_CLIENT_VERSION + "\"}},";

    static final String MUSIC_SEARCH_URL = MUSIC_BASE_URL + "/search?key=" + MUSIC_INNERTUBE_API_KEY;
    static final String MUSIC_SEARCH_PAYLOAD = MUSIC_BASE_PAYLOAD + "\"query\":\"%s\",\"params\":\"Eg-KAQwIARAAGAAgACgAMABqChADEAQQCRAFEAo=\"}";

    static final String WATCH_URL_PREFIX = YOUTUBE_ORIGIN + "/watch?v=";
}