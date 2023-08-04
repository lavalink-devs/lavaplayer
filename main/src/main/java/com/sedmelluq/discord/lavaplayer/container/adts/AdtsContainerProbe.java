package com.sedmelluq.discord.lavaplayer.container.adts;

import com.sedmelluq.discord.lavaplayer.container.MediaContainerDetection;
import com.sedmelluq.discord.lavaplayer.container.MediaContainerDetectionResult;
import com.sedmelluq.discord.lavaplayer.container.MediaContainerHints;
import com.sedmelluq.discord.lavaplayer.container.MediaContainerProbe;
import com.sedmelluq.discord.lavaplayer.tools.io.SeekableInputStream;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.info.AudioTrackInfoBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static com.sedmelluq.discord.lavaplayer.container.MediaContainerDetectionResult.supportedFormat;

/**
 * Container detection probe for ADTS stream format.
 */
public class AdtsContainerProbe implements MediaContainerProbe {
    private static final Logger log = LoggerFactory.getLogger(AdtsContainerProbe.class);

    @Override
    public String getName() {
        return "adts";
    }

    @Override
    public boolean matchesHints(MediaContainerHints hints) {
        boolean invalidMimeType = hints.mimeType != null && !"audio/aac".equalsIgnoreCase(hints.mimeType);
        boolean invalidFileExtension = hints.fileExtension != null && !"aac".equalsIgnoreCase(hints.fileExtension);
        return hints.present() && !invalidMimeType && !invalidFileExtension;
    }

    @Override
    public MediaContainerDetectionResult probe(AudioReference reference, SeekableInputStream inputStream) throws IOException {
        AdtsStreamReader reader = new AdtsStreamReader(inputStream);

        if (reader.findPacketHeader(MediaContainerDetection.STREAM_SCAN_DISTANCE) == null) {
            return null;
        }

        log.debug("Track {} is an ADTS stream.", reference.identifier);

        return supportedFormat(this, null, AudioTrackInfoBuilder.create(reference, inputStream).build());
    }

    @Override
    public AudioTrack createTrack(String parameters, AudioTrackInfo trackInfo, SeekableInputStream inputStream) {
        return new AdtsAudioTrack(trackInfo, inputStream);
    }
}
