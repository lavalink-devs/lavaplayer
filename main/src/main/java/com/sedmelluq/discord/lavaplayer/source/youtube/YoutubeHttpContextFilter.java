package com.sedmelluq.discord.lavaplayer.source.youtube;

import com.sedmelluq.discord.lavaplayer.tools.DataFormatTools;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.http.HttpContextFilter;
import com.sedmelluq.discord.lavaplayer.tools.http.HttpContextRetryCounter;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCookieStore;

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.COMMON;

public class YoutubeHttpContextFilter implements HttpContextFilter {
  private static final String ATTRIBUTE_RESET_RETRY = "isResetRetry";
  private static final HttpContextRetryCounter retryCounter = new HttpContextRetryCounter("yt-token-retry");

  private YoutubeAccessTokenTracker tokenTracker;

  public void setTokenTracker(YoutubeAccessTokenTracker tokenTracker) {
    this.tokenTracker = tokenTracker;
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
    if (!isRepetition) {
      context.removeAttribute(ATTRIBUTE_RESET_RETRY);
    }

    retryCounter.handleUpdate(context, isRepetition);

    if (tokenTracker.isTokenFetchContext(context)) {
      // Used for fetching access token, let's not recurse.
      return;
    }

    String accessToken = tokenTracker.getAccessToken();
    if (!DataFormatTools.isNullOrEmpty(accessToken)) {
      request.setHeader("Authorization", "Bearer " + accessToken);
    }
  }

  @Override
  public boolean onRequestResponse(HttpClientContext context, HttpUriRequest request, HttpResponse response) {
    if (response.getStatusLine().getStatusCode() == 429) {
      throw new FriendlyException("This IP address has been blocked by YouTube (429).", COMMON, null);
    }

    if (tokenTracker.isTokenFetchContext(context) || retryCounter.getRetryCount(context) >= 1) {
      return false;
    } else if (response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
      tokenTracker.updateAccessToken();
      return true;
    } else {
      return false;
    }
  }

  @Override
  public boolean onRequestException(HttpClientContext context, HttpUriRequest request, Throwable error) {
    // Always retry once in case of connection reset exception.
    if (HttpClientTools.isConnectionResetException(error)) {
      if (context.getAttribute(ATTRIBUTE_RESET_RETRY) == null) {
        context.setAttribute(ATTRIBUTE_RESET_RETRY, true);
        return true;
      }
    }

    return false;
  }
}
