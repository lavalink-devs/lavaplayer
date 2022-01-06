package com.sedmelluq.discord.lavaplayer.source.youtube;

public class YoutubeConstants {

    // YouTube constants
    static final String YOUTUBE_ORIGIN = "https://www.youtube.com";
    static final String YOUTUBE_API_ORIGIN = "https://youtubei.googleapis.com";
    static final String BASE_URL = YOUTUBE_API_ORIGIN + "/youtubei/v1";
    static final String INNERTUBE_API_KEY = "AIzaSyA8eiZmM1FaDVjRy-df2KTyQ_vz_yYM39w";
    static final String CLIENT_NAME = "ANDROID";
    static final String CLIENT_VERSION = "16.24";
    static final String CLIENT_SCREEN = "EMBED";
    static final String BASE_PAYLOAD = "{\"context\":{\"client\":{\"clientName\":\"" + CLIENT_NAME + "\",\"clientVersion\":\"" + CLIENT_VERSION + "\"";
    static final String SCREEN_PART_PAYLOAD = ",\"screenDensityFloat\":1,\"screenHeightPoints\":1080,\"screenPixelDensity\":1,\"screenWidthPoints\":1920";
    static final String EMBED_PART_PAYLOAD = ",\"clientScreen\":\"" + CLIENT_SCREEN + "\"},\"thirdParty\":{\"embedUrl\":\"" + YOUTUBE_ORIGIN + "\"";
    static final String CLOSE_BASE_PAYLOAD = "}},";

    static final String PLAYER_URL = BASE_URL + "/player?key=" + INNERTUBE_API_KEY;
    static final String PLAYER_PAYLOAD = BASE_PAYLOAD + SCREEN_PART_PAYLOAD + CLOSE_BASE_PAYLOAD + "\"racyCheckOk\":true,\"contentCheckOk\":true,\"videoId\":\"%s\",\"playbackContext\":{\"contentPlaybackContext\":{\"signatureTimestamp\":%s}}}";
    static final String PLAYER_EMBED_PAYLOAD = BASE_PAYLOAD + SCREEN_PART_PAYLOAD + EMBED_PART_PAYLOAD + CLOSE_BASE_PAYLOAD + "\"racyCheckOk\":true,\"contentCheckOk\":true,\"videoId\":\"%s\",\"playbackContext\":{\"contentPlaybackContext\":{\"signatureTimestamp\":%s}}}";
    static final String VERIFY_AGE_URL = BASE_URL + "/verify_age?key=" + INNERTUBE_API_KEY;
    static final String VERIFY_AGE_PAYLOAD = BASE_PAYLOAD + SCREEN_PART_PAYLOAD + CLOSE_BASE_PAYLOAD + "\"nextEndpoint\":{\"urlEndpoint\":{\"url\":\"%s\"}},\"setControvercy\":true}";
    static final String SEARCH_URL = BASE_URL + "/search?key=" + INNERTUBE_API_KEY;
    static final String SEARCH_PAYLOAD = BASE_PAYLOAD + SCREEN_PART_PAYLOAD + CLOSE_BASE_PAYLOAD + "\"query\":\"%s\",\"params\":\"EgIQAQ==\"}";
    static final String BROWSE_URL = BASE_URL + "/browse?key=" + INNERTUBE_API_KEY;
    static final String BROWSE_CONTINUATION_PAYLOAD = BASE_PAYLOAD + SCREEN_PART_PAYLOAD + CLOSE_BASE_PAYLOAD + "\"continuation\":\"%s\"}";
    static final String BROWSE_PLAYLIST_PAYLOAD = BASE_PAYLOAD + SCREEN_PART_PAYLOAD + CLOSE_BASE_PAYLOAD + "\"browseId\":\"VL%s\"}";
    static final String NEXT_URL = BASE_URL + "/next?key=" + INNERTUBE_API_KEY;
    static final String NEXT_PAYLOAD = BASE_PAYLOAD + SCREEN_PART_PAYLOAD + CLOSE_BASE_PAYLOAD + "\"videoId\":\"%s\",\"playlistId\":\"%s\"}";

    // YouTube Music constants
    static final String MUSIC_BASE_URL = "https://music.youtube.com/youtubei/v1";
    static final String MUSIC_INNERTUBE_API_KEY = "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX30";
    static final String MUSIC_CLIENT_NAME = "WEB_REMIX";
    static final String MUSIC_CLIENT_VERSION = "0.1";
    static final String MUSIC_BASE_PAYLOAD = "{\"context\":{\"client\":{\"clientName\":\"" + MUSIC_CLIENT_NAME + "\",\"clientVersion\":\"" + MUSIC_CLIENT_VERSION + "\"}},";

    static final String MUSIC_SEARCH_URL = MUSIC_BASE_URL + "/search?key=" + MUSIC_INNERTUBE_API_KEY;
    static final String MUSIC_SEARCH_PAYLOAD = MUSIC_BASE_PAYLOAD + "\"query\":\"%s\",\"params\":\"Eg-KAQwIARAAGAAgACgAMABqChADEAQQCRAFEAo=\"}";

    // Google auth constants
    static final String AUTH_URL = "https://android.googleapis.com/auth";
    static final String BASE_MASTER_TOKEN_URL = "https://youtube.minerea.su"; // https://github.com/Walkyst/YouTube-checkin
    static final String CHECKIN_ACCOUNT = BASE_MASTER_TOKEN_URL + "/checkin";
    static final String LOGIN_ACCOUNT = BASE_MASTER_TOKEN_URL + "/login";
    static final String MASTER_TOKEN_PAYLOAD = "{\"email\":\"%s\",\"password\":\"%s\"}";

    static final String WATCH_URL_PREFIX = YOUTUBE_ORIGIN + "/watch?v=";
}
