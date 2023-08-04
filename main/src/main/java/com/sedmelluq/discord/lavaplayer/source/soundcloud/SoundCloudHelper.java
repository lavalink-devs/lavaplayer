package com.sedmelluq.discord.lavaplayer.source.soundcloud;

import com.sedmelluq.discord.lavaplayer.tools.ExceptionTools;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.tools.io.PersistentHttpStream;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import org.apache.http.Header;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.protocol.HttpClientContext;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;

public class SoundCloudHelper {
    public static String nonMobileUrl(String url) {
        if (url.startsWith("https://m.")) {
            return "https://" + url.substring("https://m.".length());
        } else {
            return url;
        }
    }

    public static String loadPlaybackUrl(HttpInterface httpInterface, String jsonUrl) throws IOException {
        try (PersistentHttpStream stream = new PersistentHttpStream(httpInterface, URI.create(jsonUrl), null)) {
            if (!HttpClientTools.isSuccessWithContent(stream.checkStatusCode())) {
                throw new IOException("Invalid status code for soundcloud stream: " + stream.checkStatusCode());
            }

            JsonBrowser json = JsonBrowser.parse(stream);
            return json.get("url").text();
        }
    }

    public static AudioReference redirectMobileLink(HttpInterface httpInterface, AudioReference reference) {
        try (CloseableHttpResponse response = httpInterface.execute(new HttpGet(reference.identifier))) {
            HttpClientTools.assertSuccessWithContent(response, "mobile redirect response");
            HttpClientContext context = httpInterface.getContext();
            List<URI> redirects = context.getRedirectLocations();
            if (redirects != null && !redirects.isEmpty()) {
                return new AudioReference(redirects.get(0).toString(), null);
            } else {
                throw new FriendlyException("Unable to process soundcloud mobile link", SUSPICIOUS,
                    new IllegalStateException("Expected soundcloud to redirect soundcloud.app.goo.gl link to a valid track/playlist link, but it did not redirect at all"));
            }
        } catch (Exception e) {
            throw ExceptionTools.wrapUnfriendlyExceptions(e);
        }
    }

    public static AudioReference resolveShortTrackUrl(HttpInterface httpInterface, AudioReference reference) {
        HttpHead request = new HttpHead(reference.identifier);
        request.setConfig(RequestConfig.custom().setRedirectsEnabled(false).build());
        try (CloseableHttpResponse response = httpInterface.execute(request)) {
            Header header = response.getLastHeader("Location");
            if (header == null) {
                throw new FriendlyException("Unable to resolve Soundcloud short URL", SUSPICIOUS,
                    new IllegalStateException("Unable to locate canonical URL"));
            }
            return new AudioReference(header.getValue(), null);
        } catch (Exception e) {
            throw ExceptionTools.wrapUnfriendlyExceptions(e);
        }
    }
}
