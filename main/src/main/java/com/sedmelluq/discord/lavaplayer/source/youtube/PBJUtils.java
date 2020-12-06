package com.sedmelluq.discord.lavaplayer.source.youtube;

import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.tools.Units;

import java.util.List;

public class PBJUtils {
    protected static long parseDuration(String duration) {
        String[] parts = duration.split(":");

        if (parts.length == 3) { // hh::mm:ss
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            int seconds = Integer.parseInt(parts[2]);
            return (hours * 3600000L) + (minutes * 60000L) + (seconds * 1000L);
        } else if (parts.length == 2) { // mm:ss
            int minutes = Integer.parseInt(parts[0]);
            int seconds = Integer.parseInt(parts[1]);
            return (minutes * 60000L) + (seconds * 1000L);
        } else {
            return Units.DURATION_MS_UNKNOWN;
        }
    }

    public static String getBestThumbnail(JsonBrowser videoData, String videoId) {
        List<JsonBrowser> thumbnails = videoData.get("thumbnail").get("thumbnails").values();
        if (!thumbnails.isEmpty()) return thumbnails.get(thumbnails.size() - 1).get("url").text();
        return String.format("https://i.ytimg.com/vi_webp/%s/maxresdefault.webp", videoId);
    }
}
