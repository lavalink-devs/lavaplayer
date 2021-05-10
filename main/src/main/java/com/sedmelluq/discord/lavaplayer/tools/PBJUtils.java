package com.sedmelluq.discord.lavaplayer.tools;

import java.util.List;

public class PBJUtils {
    public static String getYoutubeMusicThumbnail(JsonBrowser videoData, String videoId) {
        JsonBrowser thumbnails = videoData.get("thumbnail").get("thumbnails").index(0);
        if (thumbnails != JsonBrowser.NULL_BROWSER) {
            String thumbnail = thumbnails.get("url").text();
            return thumbnail.replace("w60-h60-l90-rj", "w1000-h1000");
        }
        return String.format("https://i.ytimg.com/vi_webp/%s/maxresdefault.webp", videoId);
    }

    public static String getYouTubeThumbnail(JsonBrowser videoData, String videoId) {
        List<JsonBrowser> thumbnails = videoData.get("thumbnail").get("thumbnails").values();
        if (!thumbnails.isEmpty()) return thumbnails.get(thumbnails.size() - 1).get("url").text();
        return String.format("https://i.ytimg.com/vi_webp/%s/maxresdefault.webp", videoId);
    }

    public static String getSoundCloudThumbnail(JsonBrowser trackData) {
        return trackData.get("artwork_url").text().replace("large.jpg", "original.jpg");
    }
}
