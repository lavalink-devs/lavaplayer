package com.sedmelluq.discord.lavaplayer.source.youtube;

import com.sedmelluq.discord.lavaplayer.container.mpeg.MpegAudioTrack;
import com.sedmelluq.discord.lavaplayer.container.mpeg.MpegFileLoader;
import com.sedmelluq.discord.lavaplayer.container.mpeg.MpegTrackConsumer;
import com.sedmelluq.discord.lavaplayer.container.mpeg.reader.MpegFileTrackProvider;
import com.sedmelluq.discord.lavaplayer.tools.DataFormatTools;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.tools.io.SeekableInputStream;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioProcessingContext;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;
import static com.sedmelluq.discord.lavaplayer.tools.Units.CONTENT_LENGTH_UNKNOWN;

/**
 * YouTube segmented MPEG stream track. The base URL always gives the latest chunk. Every chunk contains the current
 * sequence number in it, which is used to get the sequence number of the next segment. This is repeated until YouTube
 * responds to a segment request with 204.
 */
public class YoutubeMpegStreamAudioTrack extends MpegAudioTrack {
  private static final Logger log = LoggerFactory.getLogger(YoutubeMpegStreamAudioTrack.class);
  private static final RequestConfig streamingRequestConfig = RequestConfig.custom().setSocketTimeout(3000).setConnectionRequestTimeout(3000).setConnectTimeout(3000).build();
  private static final long EMPTY_RETRY_THRESHOLD_MS = 400;
  private static final long EMPTY_RETRY_INTERVAL_MS = 50;

  private final HttpInterface httpInterface;
  private final URI signedUrl;

  /**
   * @param trackInfo Track info
   * @param httpInterface HTTP interface to use for loading segments
   * @param signedUrl URI of the base stream with signature resolved
   */
  public YoutubeMpegStreamAudioTrack(AudioTrackInfo trackInfo, HttpInterface httpInterface, URI signedUrl) {
    super(trackInfo, null);

    this.httpInterface = httpInterface;
    this.signedUrl = signedUrl;

    // YouTube does not return a segment until it is ready, this might trigger a connect timeout otherwise.
    httpInterface.getContext().setRequestConfig(streamingRequestConfig);
  }

  @Override
  public void process(LocalAudioTrackExecutor localExecutor) {
    localExecutor.executeProcessingLoop(() -> {
      execute(localExecutor);
    }, null);
  }

  private void execute(LocalAudioTrackExecutor localExecutor) throws InterruptedException {
    TrackState state = new TrackState(signedUrl);

    if (!trackInfo.isStream && state.absoluteSequence == null) {
      state.absoluteSequence = 0L;
    }

    try {
      while (!state.finished) {
        processNextSegmentWithRetry(localExecutor, state);
        state.relativeSequence++;
      }
    } finally {
      if (state.trackConsumer != null) {
        state.trackConsumer.close();
      }
    }
  }

  private void processNextSegmentWithRetry(
      LocalAudioTrackExecutor localExecutor,
      TrackState state
  ) throws InterruptedException {
    if (processNextSegment(localExecutor, state)) {
      return;
    }

    // First attempt gave empty result, possibly because the stream is not yet finished, but the next segment is just
    // not ready yet. Keep retrying at EMPTY_RETRY_INTERVAL_MS intervals until EMPTY_RETRY_THRESHOLD_MS is reached.
    long waitStart = System.currentTimeMillis();
    long iterationStart = waitStart;

    while (!processNextSegment(localExecutor, state)) {
      // EMPTY_RETRY_THRESHOLD_MS is the maximum time between the end of the first attempt and the beginning of the last
      // attempt, to avoid retry being skipped due to response coming slowly.
      if (iterationStart - waitStart >= EMPTY_RETRY_THRESHOLD_MS) {
        state.finished = true;
        break;
      } else {
        Thread.sleep(EMPTY_RETRY_INTERVAL_MS);
        iterationStart = System.currentTimeMillis();
      }
    }
  }

  private boolean processNextSegment(
      LocalAudioTrackExecutor localExecutor,
      TrackState state
  ) throws InterruptedException {
    URI segmentUrl = getNextSegmentUrl(state);

    log.debug("Segment URL: {}", segmentUrl.toString());

    try (YoutubePersistentHttpStream stream = new YoutubePersistentHttpStream(httpInterface, segmentUrl, CONTENT_LENGTH_UNKNOWN)) {
      if (stream.checkStatusCode() == HttpStatus.SC_NO_CONTENT || stream.getContentLength() == 0) {
        return false;
      }

      // If we were redirected, use that URL as a base for the next segment URL. Otherwise we will likely get redirected
      // again on every other request, which is inefficient (redirects across domains, the original URL is always
      // closing the connection, whereas the final URL is keep-alive).
      state.baseUrl = httpInterface.getFinalLocation();

      processSegmentStream(stream, localExecutor.getProcessingContext(), state);

      stream.releaseConnection();
    } catch (IOException e) {
      // IOException here usually means that stream is about to end.
      return false;
    }

    return true;
  }

  private void processSegmentStream(SeekableInputStream stream, AudioProcessingContext context, TrackState state) throws InterruptedException, IOException {
    MpegFileLoader file = new MpegFileLoader(stream);
    file.parseHeaders();

    if (!trackInfo.isStream) {
      state.absoluteSequence++;
    } else {
      state.absoluteSequence = extractAbsoluteSequenceFromEvent(file.getLastEventMessage());
    }

    if (state.trackConsumer == null) {
      state.trackConsumer = loadAudioTrack(file, context);
    }

    MpegFileTrackProvider fileReader = file.loadReader(state.trackConsumer);
    if (fileReader == null) {
      throw new FriendlyException("Unknown MP4 format.", SUSPICIOUS, null);
    }

    fileReader.provideFrames();
  }

  private URI getNextSegmentUrl(TrackState state) {
    URIBuilder builder = new URIBuilder(state.baseUrl)
        .setParameter("rn", String.valueOf(state.relativeSequence))
        .setParameter("rbuf", "0");

    if (state.absoluteSequence != null) {
      builder.setParameter("sq", String.valueOf(state.absoluteSequence + 1));
    }

    try {
      return builder.build();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  private Long extractAbsoluteSequenceFromEvent(byte[] data) {
    if (data == null) {
      return null;
    }

    String message = new String(data, StandardCharsets.UTF_8);
    String sequence = DataFormatTools.extractBetween(message, "Sequence-Number: ", "\r\n");

    return sequence != null ? Long.valueOf(sequence) : null;
  }

  private static class TrackState {
    private long relativeSequence;
    private Long absoluteSequence;
    private MpegTrackConsumer trackConsumer;
    private boolean finished;
    private URI baseUrl;

    public TrackState(URI baseUrl) {
      this.baseUrl = baseUrl;
    }
  }
}