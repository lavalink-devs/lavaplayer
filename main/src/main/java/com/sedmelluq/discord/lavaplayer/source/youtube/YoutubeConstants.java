package com.sedmelluq.discord.lavaplayer.source.youtube;

public class YoutubeConstants {

    // YouTube constants
    static final String YOUTUBE_ORIGIN = "https://www.youtube.com";
    static final String YOUTUBE_API_ORIGIN = "https://youtubei.googleapis.com";
    static final String BASE_URL = YOUTUBE_API_ORIGIN + "/youtubei/v1";
    static final String INNERTUBE_API_KEY = "AIzaSyA8eiZmM1FaDVjRy-df2KTyQ_vz_yYM39w";
    static final String CLIENT_ANDROID_NAME = "ANDROID";
    static final String CLIENT_ANDROID_VERSION = "16.24";
    static final String CLIENT_WEB_NAME = "WEB";
    static final String CLIENT_WEB_VERSION = "2.20211221.00.00";
    static final String CLIENT_TVHTML5_NAME = "TVHTML5_SIMPLY_EMBEDDED_PLAYER";
    static final String CLIENT_TVHTML5_VERSION = "2.0";
    static final String CLIENT_SCREEN_EMBED = "EMBED";
    static final String BASE_PAYLOAD = "{\"context\":{\"client\":{\"clientName\":\"%s\",\"clientVersion\":\"%s\"";
    static final String DEFAULT_BASE_PAYLOAD = String.format(BASE_PAYLOAD, CLIENT_ANDROID_NAME, CLIENT_ANDROID_VERSION);
    static final String SCREEN_PART_PAYLOAD = ",\"screenDensityFloat\":1,\"screenHeightPoints\":1080,\"screenPixelDensity\":1,\"screenWidthPoints\":1920";
    static final String EMBED_PART_PAYLOAD = ",\"clientScreen\":\"" + CLIENT_SCREEN_EMBED + "\"},\"thirdParty\":{\"embedUrl\":\"https://google.com\"";
    static final String CLOSE_BASE_PAYLOAD = "}},";
    static final String CLOSE_PLAYER_PAYLOAD = "\"racyCheckOk\":true,\"contentCheckOk\":true,\"videoId\":\"%s\",\"playbackContext\":{\"contentPlaybackContext\":{\"signatureTimestamp\":%s}}}";

    static final String SEARCH_URL = BASE_URL + "/search?key=" + INNERTUBE_API_KEY;
    static final String SEARCH_PAYLOAD = DEFAULT_BASE_PAYLOAD + SCREEN_PART_PAYLOAD + CLOSE_BASE_PAYLOAD + "\"query\":\"%s\",\"params\":\"EgIQAQ==\"}";
    static final String PLAYER_URL = BASE_URL + "/player";
    static final String PLAYER_PAYLOAD = DEFAULT_BASE_PAYLOAD + SCREEN_PART_PAYLOAD + CLOSE_BASE_PAYLOAD + CLOSE_PLAYER_PAYLOAD;
    static final String PLAYER_EMBED_PAYLOAD = DEFAULT_BASE_PAYLOAD + SCREEN_PART_PAYLOAD + EMBED_PART_PAYLOAD + CLOSE_BASE_PAYLOAD + CLOSE_PLAYER_PAYLOAD;
    static final String BROWSE_URL = BASE_URL + "/browse";
    static final String BROWSE_PLAYLIST_PAYLOAD = DEFAULT_BASE_PAYLOAD + SCREEN_PART_PAYLOAD + CLOSE_BASE_PAYLOAD + "\"browseId\":\"VL%s\"}";
    static final String BROWSE_CONTINUATION_PAYLOAD = DEFAULT_BASE_PAYLOAD + SCREEN_PART_PAYLOAD + CLOSE_BASE_PAYLOAD + "\"continuation\":\"%s\"}";
    static final String NEXT_URL = BASE_URL + "/next";
    static final String NEXT_PAYLOAD = DEFAULT_BASE_PAYLOAD + SCREEN_PART_PAYLOAD + CLOSE_BASE_PAYLOAD + "\"videoId\":\"%s\",\"playlistId\":\"%s\"}";

    // YouTube Music constants
    static final String MUSIC_BASE_URL = "https://music.youtube.com/youtubei/v1";
    static final String MUSIC_INNERTUBE_API_KEY = "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX30";
    static final String MUSIC_CLIENT_NAME = "WEB_REMIX";
    static final String MUSIC_CLIENT_VERSION = "0.1";
    static final String MUSIC_BASE_PAYLOAD = "{\"context\":{\"client\":{\"clientName\":\"" + MUSIC_CLIENT_NAME + "\",\"clientVersion\":\"" + MUSIC_CLIENT_VERSION + "\"}},";

    static final String MUSIC_SEARCH_URL = MUSIC_BASE_URL + "/search?key=" + MUSIC_INNERTUBE_API_KEY;
    static final String MUSIC_SEARCH_PAYLOAD = MUSIC_BASE_PAYLOAD + "\"query\":\"%s\",\"params\":\"Eg-KAQwIARAAGAAgACgAMABqChADEAQQCRAFEAo=\"}";

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