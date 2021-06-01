package com.sedmelluq.discord.lavaplayer.source.youtube;

public class YoutubeConstants {

    // YouTube constants
    static final String BASE_URL = "https://youtubei.googleapis.com/youtubei/v1";
    static final String INNERTUBE_API_KEY = "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8";
    static final String CLIENT_NAME = "ANDROID";
    static final String CLIENT_VERSION = "16.24";
    static final String BASE_PAYLOAD = "{\"context\":{\"client\":{\"clientName\":\"" + CLIENT_NAME + "\",\"clientVersion\":\"" + CLIENT_VERSION + "\"}},";

    static final String PLAYER_URL = BASE_URL + "/player?key=" + INNERTUBE_API_KEY;
    static final String PLAYER_PAYLOAD = BASE_PAYLOAD + "\"videoId\":\"%s\",\"playbackContext\":{\"contentPlaybackContext\":{\"signatureTimestamp\":%s}}}";
    static final String VERIFY_AGE_URL = BASE_URL + "/verify_age?key=" + INNERTUBE_API_KEY;
    static final String VERIFY_AGE_PAYLOAD = BASE_PAYLOAD + "\"nextEndpoint\":{\"urlEndpoint\":{\"url\":\"%s\"}},\"setControvercy\":true}";
    static final String SEARCH_URL = BASE_URL + "/search?key=" + INNERTUBE_API_KEY;
    static final String SEARCH_PAYLOAD = BASE_PAYLOAD + "\"query\":\"%s\",\"params\":\"EgIQAQ==\"}";

    // YouTube Music constants
    static final String MUSIC_BASE_URL = "https://music.youtube.com/youtubei/v1";
    static final String MUSIC_INNERTUBE_API_KEY = "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX30";
    static final String MUSIC_CLIENT_NAME = "WEB_REMIX";
    static final String MUSIC_CLIENT_VERSION = "1.20210524.00.00";
    static final String MUSIC_BASE_PAYLOAD = "{\"context\":{\"client\":{\"clientName\":\"" + MUSIC_CLIENT_NAME + "\",\"clientVersion\":\"" + MUSIC_CLIENT_VERSION + "\"}},";

    static final String MUSIC_SEARCH_URL = MUSIC_BASE_URL + "/search?key=" + MUSIC_INNERTUBE_API_KEY;
    static final String MUSIC_SEARCH_PAYLOAD = MUSIC_BASE_PAYLOAD + "\"query\":\"%s\",\"params\":\"Eg-KAQwIARAAGAAgACgAMABqChADEAQQCRAFEAo=\"}";

    static final String WATCH_URL_PREFIX = "https://www.youtube.com/watch?v=";
}
