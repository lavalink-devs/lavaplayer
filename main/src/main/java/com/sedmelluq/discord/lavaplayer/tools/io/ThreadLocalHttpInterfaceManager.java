package com.sedmelluq.discord.lavaplayer.tools.io;

import com.sedmelluq.discord.lavaplayer.tools.http.HttpContextFilter;
import com.sedmelluq.discord.lavaplayer.tools.http.SettableHttpRequestFilter;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

/**
 * HTTP interface manager which reuses an HttpContext by keeping it as a thread local. In case a new interface is
 * requested before the previous one has been closed, it creates a new context for the returned interface. The HTTP
 * client instance used is created lazily.
 */
public class ThreadLocalHttpInterfaceManager extends AbstractHttpInterfaceManager {
    private final ThreadLocal<HttpInterface> httpInterfaces;
    private final SettableHttpRequestFilter filter;

    /**
     * @param clientBuilder HTTP client builder to use for creating the client instance.
     * @param requestConfig Request config used by the client builder
     */
    public ThreadLocalHttpInterfaceManager(HttpClientBuilder clientBuilder, RequestConfig requestConfig) {
        super(clientBuilder, requestConfig);

        this.filter = new SettableHttpRequestFilter();
        this.httpInterfaces = ThreadLocal.withInitial(() ->
            new HttpInterface(getSharedClient(), HttpClientContext.create(), false, filter)
        );
    }

    @Override
    public HttpInterface getInterface() {
        CloseableHttpClient client = getSharedClient();

        HttpInterface httpInterface = httpInterfaces.get();
        if (httpInterface.getHttpClient() != client) {
            httpInterfaces.remove();
            httpInterface = httpInterfaces.get();
        }

        if (httpInterface.acquire()) {
            return httpInterface;
        }

        httpInterface = new HttpInterface(client, HttpClientContext.create(), false, filter);
        httpInterface.acquire();
        return httpInterface;
    }

    @Override
    public void setHttpContextFilter(HttpContextFilter modifier) {
        filter.set(modifier);
    }
}
