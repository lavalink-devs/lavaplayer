package com.sedmelluq.discord.lavaplayer.tools;

public class PBJUtils {

    public static String getYouTubeMusicThumbnail(JsonBrowser videoData, String videoId) {
        JsonBrowser thumbnails = videoData.get("thumbnail").get("thumbnails").index(0);
        if (!thumbnails.isNull()) return thumbnails.get("url").text().replaceFirst("=.*", "=w1000-h1000");
        return String.format("https://i.ytimg.com/vi_webp/%s/maxresdefault.webp", videoId);
    }

    public static String getYouTubeThumbnail(String videoId) {
        return String.format("https://i.ytimg.com/vi_webp/%s/maxresdefault.webp", videoId);
    }

    public static String getSoundCloudThumbnail(JsonBrowser trackData) {
        JsonBrowser thumbnail = trackData.get("artwork_url");
        if (!thumbnail.isNull()) return thumbnail.text().replace("large.jpg", "original.jpg");
        JsonBrowser avatar = trackData.get("user").get("avatar_url");
        return avatar.text().replace("large.jpg", "original.jpg");
    }
}
