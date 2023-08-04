package com.sedmelluq.discord.lavaplayer.source.youtube;

import org.json.JSONObject;

public class YoutubePayloadHelper {

    public static JSONObject putOnceAndJoin(JSONObject json, String key) {
        if (key != null) {
            if (json.opt(key) != null) {
                return json.getJSONObject(key);
            }
            return json.put(key, new JSONObject()).getJSONObject(key);
        }
        return json.getJSONObject(null);
    }
}
