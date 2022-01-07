package com.sedmelluq.discord.lavaplayer.source.youtube;

import com.sedmelluq.discord.lavaplayer.tools.DataFormatTools;
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterfaceManager;
import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeConstants.AUTH_URL;
import static com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeConstants.CHECKIN_ACCOUNT;
import static com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeConstants.LOGIN_ACCOUNT;
import static com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeConstants.MASTER_TOKEN_PAYLOAD;
import static com.sedmelluq.discord.lavaplayer.tools.DataFormatTools.convertToMapLayout;
import static java.nio.charset.StandardCharsets.UTF_8;

public class YoutubeAccessTokenTracker {
  private static final Logger log = LoggerFactory.getLogger(YoutubeAccessTokenTracker.class);

  private static final String TOKEN_FETCH_CONTEXT_ATTRIBUTE = "yt-raw";
  private static final long MASTER_TOKEN_REFRESH_INTERVAL = TimeUnit.DAYS.toMillis(7); // Not sure how long it is active after receiving, so let it be 7 days for now
  private static final long DEFAULT_ACCESS_TOKEN_REFRESH_INTERVAL = TimeUnit.HOURS.toMillis(1);

  private final Object tokenLock = new Object();
  private final HttpInterfaceManager httpInterfaceManager;
  private final String email;
  private final String password;
  private String masterToken;
  private String accessToken;
  private long lastMasterTokenUpdate;
  private long lastAccessTokenUpdate;
  private long accessTokenRefreshInterval = DEFAULT_ACCESS_TOKEN_REFRESH_INTERVAL;
  private boolean loggedAgeRestrictionsWarning = false;

  public YoutubeAccessTokenTracker(HttpInterfaceManager httpInterfaceManager, String email, String password) {
    this.httpInterfaceManager = httpInterfaceManager;
    this.email = email;
    this.password = password;
  }

  /**
   * Updates the master token if more than {@link #MASTER_TOKEN_REFRESH_INTERVAL} time has passed since last updated.
   */
  public void updateMasterToken() {
    synchronized (tokenLock) {
      if (DataFormatTools.isNullOrEmpty(email) && DataFormatTools.isNullOrEmpty(password)) {
        if (!loggedAgeRestrictionsWarning) {
          log.warn("YouTube auth tokens can't be retrieved because email and password is not set in YoutubeAudioSourceManager, age restricted videos will throw exceptions");
          loggedAgeRestrictionsWarning = true;
        }
        return;
      }

      if (loggedAgeRestrictionsWarning) return;

      long now = System.currentTimeMillis();
      if (now - lastMasterTokenUpdate < MASTER_TOKEN_REFRESH_INTERVAL) {
        log.debug("YouTube master token was recently updated, not updating again right away.");
        return;
      }

      lastMasterTokenUpdate = now;
      log.info("Updating YouTube master token (current is {}).", masterToken);

      try {
        fetchMasterToken();
        log.info("Updating YouTube master token succeeded, new token is {}.", masterToken);
      } catch (Exception e) {
        log.error("YouTube master token update failed.", e);
      }
    }
  }

  /**
   * Updates the access token if more than {@link #accessTokenRefreshInterval} time has passed since last updated.
   */
  public void updateAccessToken() {
    synchronized (tokenLock) {
      if (DataFormatTools.isNullOrEmpty(email) && DataFormatTools.isNullOrEmpty(password)) {
        if (!loggedAgeRestrictionsWarning) {
          log.warn("YouTube auth tokens can't be retrieved because email and password is not set in YoutubeAudioSourceManager, age restricted videos will throw exceptions");
          loggedAgeRestrictionsWarning = true;
        }
        return;
      }

      getMasterToken();

      if (DataFormatTools.isNullOrEmpty(masterToken) && loggedAgeRestrictionsWarning) {
        return;
      }

      long now = System.currentTimeMillis();
      if (now - lastAccessTokenUpdate < accessTokenRefreshInterval) {
        log.debug("YouTube access token was recently updated, not updating again right away.");
        return;
      }

      lastAccessTokenUpdate = now;
      log.info("Updating YouTube access token (current is {}).", accessToken);

      try {
        fetchAccessToken();
        log.info("Updating YouTube access token succeeded, new token is {}, next update will be after {} seconds.",
                accessToken,
                TimeUnit.MILLISECONDS.toSeconds(accessTokenRefreshInterval)
        );
      } catch (Exception e) {
        log.error("YouTube access token update failed.", e);
      }
    }
  }

  public String getMasterToken() {
    synchronized (tokenLock) {
      if (masterToken == null) {
        updateMasterToken();
      }

      return masterToken;
    }
  }

  public String getAccessToken() {
    synchronized (tokenLock) {
      if (accessToken == null) {
        updateAccessToken();
      }

      return accessToken;
    }
  }

  public boolean isTokenFetchContext(HttpClientContext context) {
    return context.getAttribute(TOKEN_FETCH_CONTEXT_ATTRIBUTE) == Boolean.TRUE;
  }

  private void fetchMasterToken() throws IOException {
    try (HttpInterface httpInterface = httpInterfaceManager.getInterface()) {
      httpInterface.getContext().setAttribute(TOKEN_FETCH_CONTEXT_ATTRIBUTE, true);

      masterToken = requestMasterToken(httpInterface);
    }
  }

  private void fetchAccessToken() throws IOException {
    try (HttpInterface httpInterface = httpInterfaceManager.getInterface()) {
      httpInterface.getContext().setAttribute(TOKEN_FETCH_CONTEXT_ATTRIBUTE, true);

      accessToken = requestAccessToken(httpInterface);
    }
  }

  private String requestMasterToken(HttpInterface httpInterface) throws IOException {
    HttpPost masterTokenPost = new HttpPost(LOGIN_ACCOUNT);
    StringEntity masterTokenPayload = new StringEntity(String.format(MASTER_TOKEN_PAYLOAD, email, password));
    masterTokenPost.setEntity(masterTokenPayload);

    try (CloseableHttpResponse masterTokenResponse = httpInterface.execute(masterTokenPost)) {
      String responseText = EntityUtils.toString(masterTokenResponse.getEntity(), UTF_8);
      JsonBrowser jsonBrowser = JsonBrowser.parse(responseText);

      if (masterTokenResponse.getStatusLine().getStatusCode() == 400) {
        loggedAgeRestrictionsWarning = true;
      }

      HttpClientTools.assertSuccessWithContent(masterTokenResponse, "login account response [" + jsonBrowser.get("exception").safeText() + "]");

      String services = jsonBrowser.get("services").text();
      if (!jsonBrowser.get("continueUrl").isNull()) {
        return continueUrl(httpInterface, jsonBrowser);
      } else if (!services.contains("android") || !services.contains("youtube")) {
        createAndroidAccount(httpInterface, jsonBrowser);
      }

      return jsonBrowser.get("aas_et").text();
    }
  }

  private String requestAccessToken(HttpInterface httpInterface) throws IOException {
    List<NameValuePair> params = new ArrayList<>();
    params.add(new BasicNameValuePair("app", "com.google.android.youtube"));
    params.add(new BasicNameValuePair("client_sig", "24bb24c05e47e0aefa68a58a766179d9b613a600"));
    params.add(new BasicNameValuePair("google_play_services_version", "214516005"));
    params.add(new BasicNameValuePair("service", "oauth2:https://www.googleapis.com/auth/youtube"));
    params.add(new BasicNameValuePair("Token", masterToken));
    HttpPost post = new HttpPost(buildUri(AUTH_URL, params));

    try (CloseableHttpResponse response = httpInterface.execute(post)) {
      HttpClientTools.assertSuccessWithContent(response, "access token response");
      response.getEntity().getContent();

      Map<String, String> map = convertToMapLayout(EntityUtils.toString(response.getEntity()));
      accessTokenRefreshInterval = TimeUnit.SECONDS.toMillis(Long.parseLong(map.get("ExpiresInDurationSec")));
      return map.get("Auth");
    }
  }

  private void createAndroidAccount(HttpInterface httpInterface, JsonBrowser jsonBrowser) throws IOException {
    log.info("Account " + jsonBrowser.get("email").text() + " don't have Android profile, creating new one...");

    HttpPost post = new HttpPost(CHECKIN_ACCOUNT);
    StringEntity payload = new StringEntity(String.format(MASTER_TOKEN_PAYLOAD, email, password));
    post.setEntity(payload);

    try (CloseableHttpResponse response = httpInterface.execute(post)) {
      HttpClientTools.assertSuccessWithContent(response, "creating android profile response");
    }
  }

  private String continueUrl(HttpInterface httpInterface, JsonBrowser jsonBrowser) throws IOException {
    log.warn("Not successful attempt to login into account " + jsonBrowser.get("email").text() + ", trying obtain oauth2 token through continue url...");

    HttpPost post = new HttpPost(jsonBrowser.get("continueUrl").text());
    RequestConfig config = RequestConfig.custom().setCookieSpec(CookieSpecs.NETSCAPE).setRedirectsEnabled(true).build();
    post.setConfig(config);

    try (CloseableHttpResponse response = httpInterface.execute(post)) {
      HttpClientTools.assertSuccessWithRedirectContent(response, "oauth2 redirect response");

      URI redirect = httpInterface.getFinalLocation();
      try (CloseableHttpResponse redirectResponse = httpInterface.execute(new HttpGet(redirect))) {
        return exchangeOAuth2Token(httpInterface, redirectResponse);
      }
    }
  }

  private String exchangeOAuth2Token(HttpInterface httpInterface, CloseableHttpResponse response) throws IOException {
    for (Header header : response.getAllHeaders()) {
      if (header.getName().contains("Set-Cookie") && header.getValue().contains("oauth_token")) {
        String oauthToken = DataFormatTools.extractBetween(header.toString(), "oauth_token=", ";");

        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("Token", oauthToken));
        params.add(new BasicNameValuePair("ACCESS_TOKEN", "1"));
        params.add(new BasicNameValuePair("service", "ac2dm"));
        HttpPost post = new HttpPost(buildUri(AUTH_URL, params));
        try (CloseableHttpResponse exchangeResponse = httpInterface.execute(post)) {
          HttpClientTools.assertSuccessWithContent(exchangeResponse, "exchange oauth2 token response");

          Map<String, String> map = convertToMapLayout(EntityUtils.toString(exchangeResponse.getEntity()));
          return map.get("Token");
        }
      }
    }

    throw new RuntimeException("Can't find oauth_token in continueUrl response");
  }

  private URI buildUri(String url, List<NameValuePair> params) {
    try {
      return new URIBuilder(url)
              .addParameters(params)
              .build();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }
}
