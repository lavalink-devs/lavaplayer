package com.sedmelluq.discord.lavaplayer.source.youtube;

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.http.HttpContextFilter;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCookieStore;

import static com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeConstants.YOUTUBE_ORIGIN;
import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.COMMON;

public class YoutubeHttpContextFilter implements HttpContextFilter {
  private static final String ATTRIBUTE_RESET_RETRY = "isResetRetry";

  private static String PAPISID = "HElVHkUVenb2eFXx/AhvhxMhD_KPsM4nZE";
  private static String PSID = "8Qc_mMTGhpfQdTm1-fdKq6rh9KNCUC9OONEP44RAQkvVrQrFDkgjRaj6vJdchtNXMrWd4w.";
  private static String PSIDCC = "AJi4QfE9ix2TVKVWZzmswEkeDpCcZnuScw9N2pu2dS2fGx1Nyrtv_uDH4vvaiujL82_Ys1OO";

  public static void setPAPISID(String value) {
    PAPISID = value;
  }

  public static void setPSID(String value) {
    PSID = value;
  }

  public static void setPSIDCC(String value) {
    PSIDCC = value;
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
    long millis = System.currentTimeMillis();
    String SAPISIDHASH = DigestUtils.sha1Hex(millis + " " + PAPISID + " " + YOUTUBE_ORIGIN);

    request.setHeader("Cookie",
            "__Secure-3PAPISID=" + PAPISID + " " +
            "__Secure-3PSID=" + PSID + " " +
            "__Secure-3PSIDCC=" + PSIDCC);
    request.setHeader("Origin", YOUTUBE_ORIGIN);
    request.setHeader("Authorization", "SAPISIDHASH " + millis + "_" + SAPISIDHASH);
  }

  @Override
  public boolean onRequestResponse(HttpClientContext context, HttpUriRequest request, HttpResponse response) {
    if (response.getStatusLine().getStatusCode() == 429) {
      throw new FriendlyException("This IP address has been blocked by YouTube (429).", COMMON, null);
    }

    return false;
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
