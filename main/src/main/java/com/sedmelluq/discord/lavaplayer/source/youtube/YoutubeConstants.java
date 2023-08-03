package com.sedmelluq.discord.lavaplayer.source.youtube;

public class YoutubeConstants {

    // YouTube constants
    static final String YOUTUBE_ORIGIN = "https://www.youtube.com";
    static final String YOUTUBE_API_ORIGIN = "https://youtubei.googleapis.com";
    static final String BASE_URL = YOUTUBE_API_ORIGIN + "/youtubei/v1";

    static final String INNERTUBE_ANDROID_API_KEY = "AIzaSyA8eiZmM1FaDVjRy-df2KTyQ_vz_yYM39w";
    static final String CLIENT_ANDROID_NAME = "ANDROID";
    static final String CLIENT_ANDROID_VERSION = "17.29.34";

    static final String INNERTUBE_WEB_API_KEY = "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8";
    static final String CLIENT_WEB_NAME = "WEB";
    static final String CLIENT_WEB_VERSION = "2.20220801.00.00";

    static final String INNERTUBE_TV_API_KEY = "AIzaSyD-L7DIyuMgBk-B4DYmjJZ5UG-D6Y-vkMc";
    static final String CLIENT_TVHTML5_NAME = "TVHTML5_SIMPLY_EMBEDDED_PLAYER";
    static final String CLIENT_TVHTML5_VERSION = "2.0";

    static final String CLIENT_SCREEN_EMBED = "EMBED";
    static final String CLIENT_THIRD_PARTY_EMBED = "https://google.com";
    static final String PLAYER_PARAMS = "CgIQBg";
    static final String SEARCH_PARAMS = "EgIQAUICCAE=";

    static final String SEARCH_URL = BASE_URL + "/search?key=" + INNERTUBE_ANDROID_API_KEY;
    static final String PLAYER_URL = BASE_URL + "/player";
    static final String BROWSE_URL = BASE_URL + "/browse";
    static final String NEXT_URL = BASE_URL + "/next";
    static final String VISITOR_ID_URL = BASE_URL + "/visitor_id";

    // YouTube Music constants
    static final String BASE_MUSIC_URL = "https://music.youtube.com/youtubei/v1";

    static final String INNERTUBE_MUSIC_API_KEY = "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX30";
    static final String CLIENT_MUSIC_NAME = "WEB_REMIX";
    static final String CLIENT_MUSIC_VERSION = "0.1";

    static final String SEARCH_MUSIC_PARAMS = "Eg-KAQwIARAAGAAgACgAMABqChADEAQQCRAFEAo=";

    static final String MUSIC_SEARCH_URL = BASE_MUSIC_URL + "/search?key=" + INNERTUBE_MUSIC_API_KEY;

    // YouTube TV auth constants
    static final String TV_AUTH_BASE_URL = YOUTUBE_ORIGIN + "/o/oauth2";
    static final String TV_AUTH_SCOPE = "http://gdata.youtube.com https://www.googleapis.com/auth/youtube-paid-content";
    static final String TV_AUTH_MODEL_NAME = "ytlr::";

    static final String TV_AUTH_CODE_URL = TV_AUTH_BASE_URL + "/device/code";
    static final String TV_AUTH_CODE_PAYLOAD = "{\"client_id\":\"%s\",\"device_id\":\"%s\",\"scope\":\"" + TV_AUTH_SCOPE + "\",\"model_name\":\"" + TV_AUTH_MODEL_NAME + "\"}";
    static final String TV_AUTH_TOKEN_URL = TV_AUTH_BASE_URL + "/token";
    static final String TV_AUTH_TOKEN_PAYLOAD = "{\"client_id\":\"%s\",\"client_secret\":\"%s\",\"code\":\"%s\",\"grant_type\":\"http://oauth.net/grant_type/device/1.0\"}";
    static final String TV_AUTH_TOKEN_REFRESH_PAYLOAD = "{\"client_id\":\"%s\",\"client_secret\":\"%s\",\"refresh_token\":\"%s\",\"grant_type\":\"refresh_token\"}";

    // Android auth constants
    static final String ANDROID_AUTH_URL = "https://android.googleapis.com/auth";
    static final String MASTER_TOKEN_BASE_URL = "https://youtube.minerea.su"; // https://github.com/Walkyst/YouTube-checkin
    static final String TOKEN_BASE_PAYLOAD = "{\"email\":\"%s\",\"password\":\"%s\"";
    static final String REFRESH_PART_PAYLOAD = ",\"refresh_token\":\"%s\"";
    static final String CLOSE_TOKEN_BASE_PAYLOAD = "}";

    static final String CHECKIN_ACCOUNT_URL = MASTER_TOKEN_BASE_URL + "/checkin";
    static final String LOGIN_ACCOUNT_URL = MASTER_TOKEN_BASE_URL + "/login";
    static final String SAVE_ACCOUNT_URL = MASTER_TOKEN_BASE_URL + "/tv";
    static final String TOKEN_PAYLOAD = TOKEN_BASE_PAYLOAD + CLOSE_TOKEN_BASE_PAYLOAD;
    static final String TOKEN_REFRESH_PAYLOAD = TOKEN_BASE_PAYLOAD + REFRESH_PART_PAYLOAD + CLOSE_TOKEN_BASE_PAYLOAD;

    // Utility constants
    static final String WATCH_URL_PREFIX = YOUTUBE_ORIGIN + "/watch?v=";
}
