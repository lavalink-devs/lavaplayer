package com.sedmelluq.discord.lavaplayer.source.nico;

import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.tools.io.PersistentHttpStream;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * An extension of PersistentHttpStream that allows for sending heartbeats to a secondary URL.
 */
public class HeartbeatingHttpStream extends PersistentHttpStream {
    private static final Logger log = LoggerFactory.getLogger(HeartbeatingHttpStream.class);
    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    private String heartbeatUrl;
    private int heartbeatInterval;
    private String heartbeatPayload;

    private ScheduledFuture<?> heartbeatFuture;

    /**
     * Creates a new heartbeating http stream.
     * @param httpInterface The HTTP interface to use for requests.
     * @param contentUrl The URL to play from.
     * @param contentLength The length of the content. Null if unknown.
     * @param heartbeatUrl The URL to send heartbeat requests to.
     * @param heartbeatInterval The interval at which to heartbeat, in milliseconds.
     * @param heartbeatPayload The initial heartbeat payload.
     */
    public HeartbeatingHttpStream(
        HttpInterface httpInterface,
        URI contentUrl,
        Long contentLength,
        String heartbeatUrl,
        int heartbeatInterval,
        String heartbeatPayload
    ) {
        super(httpInterface, contentUrl, contentLength);

        this.heartbeatUrl = heartbeatUrl;
        this.heartbeatInterval = heartbeatInterval;
        this.heartbeatPayload = heartbeatPayload;

        setupHeartbeat();
    }

    protected void setupHeartbeat() {
        log.debug("Heartbeat every {} milliseconds to URL: {}", heartbeatInterval, heartbeatUrl);

        heartbeatFuture = executor.scheduleAtFixedRate(() -> {
            try {
                sendHeartbeat();
            } catch (Throwable t) {
                log.error("Heartbeat error!", t);
                IOUtils.closeQuietly(this);
            }
        }, heartbeatInterval, heartbeatInterval, TimeUnit.MILLISECONDS);
    }

    protected void sendHeartbeat() throws IOException {
        HttpPost request = new HttpPost(heartbeatUrl);
        request.addHeader("Host", "api.dmc.nico");
        request.addHeader("Connection", "keep-alive");
        request.addHeader("Content-Type", "application/json");
        request.addHeader("Origin", "https://www.nicovideo.jp");
        request.setEntity(new StringEntity(heartbeatPayload));

        try (CloseableHttpResponse response = httpInterface.execute(request)) {
            HttpClientTools.assertSuccessWithContent(response, "heartbeat page");

            heartbeatPayload = JsonBrowser.parse(response.getEntity().getContent()).get("data").format();
        }
    }

    @Override
    public void close() throws IOException {
        heartbeatFuture.cancel(false);
        super.close();
    }
}
