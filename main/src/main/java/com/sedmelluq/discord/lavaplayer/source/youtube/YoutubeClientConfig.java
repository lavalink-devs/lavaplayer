package com.sedmelluq.discord.lavaplayer.source.youtube;

import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import org.json.JSONObject;

import static com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeConstants.*;
import static com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeHttpContextFilter.ATTRIBUTE_USER_AGENT_SPECIFIED;
import static com.sedmelluq.discord.lavaplayer.source.youtube.YoutubePayloadHelper.putOnceAndJoin;

public class YoutubeClientConfig extends JSONObject {

    public static YoutubeClientConfig IOS = new YoutubeClientConfig()
        .withApiKey(INNERTUBE_IOS_API_KEY)
        .withUserAgent("com.google.ios.youtube/19.09.3 (iPhone14,3; U; CPU iOS 15_6 like Mac OS X)")
        .withClientName(CLIENT_IOS_NAME)
        .withClientField("clientVersion", CLIENT_IOS_VERSION)
        .withClientField("deviceModel", "iPhone14,3")
        //.withClientField("osName", "Android")
        //.withClientField("osVersion", DEFAULT_ANDROID_VERSION.getOsVersion())
        .withClientDefaultScreenParameters();

    public static YoutubeClientConfig TV_EMBEDDED = new YoutubeClientConfig()
        .withApiKey(INNERTUBE_WEB_API_KEY) //.withApiKey(INNERTUBE_TV_API_KEY) // Requires header (Referer tv.youtube.com)
        .withClientName(CLIENT_TVHTML5_NAME)
        .withClientField("clientVersion", CLIENT_TVHTML5_VERSION)
        .withClientField("clientScreen", CLIENT_SCREEN_EMBED)
        .withClientDefaultScreenParameters()
        .withThirdPartyEmbedUrl(CLIENT_THIRD_PARTY_EMBED);

    public static YoutubeClientConfig WEB = new YoutubeClientConfig()
        .withApiKey(INNERTUBE_WEB_API_KEY)
        .withClientName(CLIENT_WEB_NAME)
        .withClientField("clientVersion", CLIENT_WEB_VERSION);

    public static YoutubeClientConfig MUSIC = new YoutubeClientConfig()
        .withApiKey(INNERTUBE_MUSIC_API_KEY) // Requires header (Referer music.youtube.com)
        .withClientName(CLIENT_MUSIC_NAME)
        .withClientField("clientVersion", CLIENT_MUSIC_VERSION);

    private String name;
    private String userAgent;
    private String apiKey;
    private final JSONObject root;

    public YoutubeClientConfig() {
        this.root = new JSONObject();
        this.userAgent = null;
        this.name = null;
    }

    private YoutubeClientConfig(JSONObject context, String userAgent, String name) {
        this.root = context;
        this.userAgent = userAgent;
        this.name = name;
    }

    public YoutubeClientConfig copy() {
        return new YoutubeClientConfig(new JSONObject(root.toMap()), userAgent, name);
    }

    public YoutubeClientConfig withClientName(String name) {
        this.name = name;
        withClientField("clientName", name);
        return this;
    }

    public String getName() {
        return name;
    }

    public YoutubeClientConfig withUserAgent(String userAgent) {
        this.userAgent = userAgent;
        return this;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public YoutubeClientConfig withApiKey(String apiKey) {
        this.apiKey = apiKey;
        return this;
    }

    public String getApiKey() {
        return apiKey;
    }

    public YoutubeClientConfig withClientDefaultScreenParameters() {
        withClientField("screenDensityFloat", 1);
        withClientField("screenHeightPoints", 1080);
        withClientField("screenPixelDensity", 1);
        return withClientField("screenWidthPoints", 1920);
    }

    public YoutubeClientConfig withThirdPartyEmbedUrl(String embedUrl) {
        JSONObject context = putOnceAndJoin(root, "context");
        JSONObject thirdParty = putOnceAndJoin(context, "thirdParty");
        thirdParty.put("embedUrl", embedUrl);
        return this;
    }

    public YoutubeClientConfig withPlaybackSignatureTimestamp(String signatureTimestamp) {
        JSONObject playbackContext = putOnceAndJoin(root, "playbackContext");
        JSONObject contentPlaybackContext = putOnceAndJoin(playbackContext, "contentPlaybackContext");
        contentPlaybackContext.put("signatureTimestamp", signatureTimestamp);
        return this;
    }

    public YoutubeClientConfig withRootField(String key, Object value) {
        root.put(key, value);
        return this;
    }

    public YoutubeClientConfig withClientField(String key, Object value) {
        JSONObject context = putOnceAndJoin(root, "context");
        JSONObject client = putOnceAndJoin(context, "client");
        client.put(key, value);
        return this;
    }

    public YoutubeClientConfig withUserField(String key, Object value) {
        JSONObject context = putOnceAndJoin(root, "context");
        JSONObject user = putOnceAndJoin(context, "user");
        user.put(key, value);
        return this;
    }

    public YoutubeClientConfig setAttribute(HttpInterface httpInterface) {
        if (userAgent != null)
            httpInterface.getContext().setAttribute(ATTRIBUTE_USER_AGENT_SPECIFIED, userAgent);
        return this;
    }

    public String toJsonString() {
        return root.toString();
    }
}
