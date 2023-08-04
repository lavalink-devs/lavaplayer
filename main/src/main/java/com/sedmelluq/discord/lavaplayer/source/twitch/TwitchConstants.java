package com.sedmelluq.discord.lavaplayer.source.twitch;

public class TwitchConstants {
    static final String TWITCH_GRAPHQL_BASE_URL = "https://gql.twitch.tv/gql";
    static final String METADATA_PAYLOAD = "{\"operationName\":\"StreamMetadata\",\"variables\":{\"channelLogin\":\"%s\"},\"extensions\":{\"persistedQuery\":{\"version\":1,\"sha256Hash\":\"1c719a40e481453e5c48d9bb585d971b8b372f8ebb105b17076722264dfa5b3e\"}}}";
    static final String ACCESS_TOKEN_PAYLOAD = "{\"operationName\":\"PlaybackAccessToken_Template\",\"query\":\"query PlaybackAccessToken_Template($login: String!,$isLive:Boolean!,$vodID:ID!,$isVod:Boolean!,$playerType:String!){streamPlaybackAccessToken(channelName:$login,params:{platform:\\\"web\\\",playerBackend:\\\"mediaplayer\\\",playerType:$playerType})@include(if:$isLive){value signature __typename}videoPlaybackAccessToken(id:$vodID,params:{platform:\\\"web\\\",playerBackend:\\\"mediaplayer\\\",playerType:$playerType})@include(if:$isVod){value signature __typename}}\",\"variables\":{\"isLive\":true,\"login\":\"%s\",\"isVod\":false,\"vodID\":\"\",\"playerType\":\"site\"}}";
}
