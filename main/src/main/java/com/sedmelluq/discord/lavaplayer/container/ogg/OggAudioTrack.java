package com.sedmelluq.discord.lavaplayer.container.ogg;

import com.sedmelluq.discord.lavaplayer.tools.io.SeekableInputStream;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.BaseAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Audio track which handles an OGG stream.
 */
public class OggAudioTrack extends BaseAudioTrack {
  private static final Logger log = LoggerFactory.getLogger(OggAudioTrack.class);

  private final SeekableInputStream inputStream;

  /**
   * @param trackInfo Track info
   * @param inputStream Input stream for the OGG stream
   */
  public OggAudioTrack(AudioTrackInfo trackInfo, SeekableInputStream inputStream) {
    super(trackInfo);

    this.inputStream = inputStream;
  }

  @Override
  public void process(final LocalAudioTrackExecutor localExecutor) throws IOException {
    OggPacketInputStream packetInputStream = new OggPacketInputStream(inputStream, false);
    OggTrackBlueprint blueprint = OggTrackLoader.loadTrackBlueprint(packetInputStream);

    if (blueprint == null) {
      throw new IOException("Stream terminated before the first packet.");
    }

    OggTrackHandler handler = blueprint.loadTrackHandler(packetInputStream);

    log.debug("Starting to play an OGG track {}", getIdentifier());

    handler.initialise(localExecutor.getProcessingContext(), 0, 0);
    localExecutor.executeProcessingLoop(handler::provideFrames, handler::seekToTimecode, true);
  }
}