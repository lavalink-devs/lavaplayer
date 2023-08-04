package com.sedmelluq.discord.lavaplayer.source.yamusic;

import com.sedmelluq.discord.lavaplayer.tools.DataFormatTools;
import com.sedmelluq.discord.lavaplayer.tools.ExceptionTools;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.FAULT;
import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;

public class DefaultYandexMusicDirectUrlLoader extends AbstractYandexMusicApiLoader implements YandexMusicDirectUrlLoader {

    private static final String TRACK_DOWNLOAD_INFO = "https://api.music.yandex.net/tracks/%s/download-info";
    private static final String DIRECT_URL_FORMAT = "https://%s/get-%s/%s/%s%s";
    private static final String MP3_SALT = "XGRlBW9FXlekgbPrRHuSiA";

    @Override
    public String getDirectUrl(String trackId, String codec) {
        return extractFromApi(String.format(TRACK_DOWNLOAD_INFO, trackId), (httpClient, codecsList) -> {
            JsonBrowser codecResult = codecsList.values().stream()
                .filter(e -> codec.equals(e.get("codec").text()))
                .findFirst()
                .orElse(null);
            if (codecResult == null) {
                throw new FriendlyException("Couldn't find supported track format.", SUSPICIOUS, null);
            }
            String storageUrl = codecResult.get("downloadInfoUrl").text();
            DownloadInfo info = extractDownloadInfo(storageUrl);

            String sign = DigestUtils.md5Hex(MP3_SALT + info.path.substring(1) + info.s);

            return String.format(DIRECT_URL_FORMAT, info.host, codec, sign, info.ts, info.path);
        });
    }

    private DownloadInfo extractDownloadInfo(String storageUrl) throws IOException {
        try (HttpInterface httpInterface = httpInterfaceManager.getInterface()) {
            String responseText;
            try (CloseableHttpResponse response = httpInterface.execute(new HttpGet(storageUrl))) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode != 200) {
                    throw new IOException("Invalid status code for track storage info: " + statusCode);
                }
                responseText = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            }
            DownloadInfo info = new DownloadInfo();
            info.host = DataFormatTools.extractBetween(responseText, "<host>", "</host>");
            info.path = DataFormatTools.extractBetween(responseText, "<path>", "</path>");
            info.ts = DataFormatTools.extractBetween(responseText, "<ts>", "</ts>");
            info.region = DataFormatTools.extractBetween(responseText, "<region>", "</region>");
            info.s = DataFormatTools.extractBetween(responseText, "<s>", "</s>");
            return info;
        } catch (Exception e) {
            throw ExceptionTools.wrapUnfriendlyExceptions("Loading information for a Yandex Music track failed.", FAULT, e);
        }
    }

    private class DownloadInfo {
        String host;
        String path;
        String ts;
        String region;
        String s;
    }
}
