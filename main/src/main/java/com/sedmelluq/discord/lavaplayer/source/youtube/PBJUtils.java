package com.sedmelluq.discord.lavaplayer.source.youtube;

import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;

import java.util.List;

public class PBJUtils {
    public static String getBestThumbnail(JsonBrowser videoData, String videoId) {
        List<JsonBrowser> thumbnails = videoData.get("thumbnail").get("thumbnails").values();
        if (!thumbnails.isEmpty()) return thumbnails.get(thumbnails.size() - 1).get("url").text();
        return String.format("https://i.ytimg.com/vi_webp/%s/maxresdefault.webp", videoId);
    }
}
