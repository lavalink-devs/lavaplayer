package com.sedmelluq.discord.lavaplayer.source.yamusic;

import com.sedmelluq.discord.lavaplayer.tools.http.HttpContextFilter;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCookieStore;

public class YandexHttpContextFilter implements HttpContextFilter {

    private static String oAuthToken = null;

    public static void setOAuthToken(String value) {
        oAuthToken = value;
    }

    @Override
    public void onContextOpen(HttpClientContext context) {
        CookieStore cookieStore = context.getCookieStore();

        if (cookieStore == null) {
            cookieStore = new BasicCookieStore();
            context.setCookieStore(cookieStore);
        }

        // Reset cookies for each sequence of requests.
        cookieStore.clear();
    }

    @Override
    public void onContextClose(HttpClientContext context) {

    }

    @Override
    public void onRequest(HttpClientContext context, HttpUriRequest request, boolean isRepetition) {
        request.setHeader("User-Agent", "Yandex-Music-API");
        request.setHeader("X-Yandex-Music-Client", "WindowsPhone/3.20");
        if (oAuthToken != null) {
            request.setHeader("Authorization", "OAuth " + oAuthToken);
        }
    }

    @Override
    public boolean onRequestResponse(HttpClientContext context, HttpUriRequest request, HttpResponse response) {
        return false;
    }

    @Override
    public boolean onRequestException(HttpClientContext context, HttpUriRequest request, Throwable error) {
        return false;
    }
}
