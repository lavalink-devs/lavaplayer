package com.sedmelluq.discord.lavaplayer.container.ogg.flac;

import com.sedmelluq.discord.lavaplayer.container.flac.*;
import com.sedmelluq.discord.lavaplayer.container.ogg.*;
import com.sedmelluq.discord.lavaplayer.tools.io.ByteBufferInputStream;
import com.sedmelluq.discord.lavaplayer.tools.io.DirectBufferStreamBroker;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Loader for an OGG FLAC track from an OGG packet stream.
 */
public class OggFlacCodecHandler implements OggCodecHandler {
    private static final int FLAC_IDENTIFIER = ByteBuffer.wrap(new byte[]{0x7F, 'F', 'L', 'A'}).getInt();

    private static final int NATIVE_FLAC_HEADER_OFFSET = 9;
    private static final int NATIVE_FLAC_HEADER = ByteBuffer.wrap(new byte[]{'f', 'L', 'a', 'C'}).getInt();

    @Override
    public boolean isMatchingIdentifier(int identifier) {
        return identifier == FLAC_IDENTIFIER;
    }

    @Override
    public int getMaximumFirstPacketLength() {
        return NATIVE_FLAC_HEADER_OFFSET + 4 + FlacMetadataHeader.LENGTH + FlacStreamInfo.LENGTH;
    }

    @Override
    public OggTrackBlueprint loadBlueprint(OggPacketInputStream stream, DirectBufferStreamBroker broker) throws IOException {
        FlacTrackInfo info = load(stream, broker);
        stream.setSeekPoints(stream.createSeekTable(info.stream.sampleRate));
        return new Blueprint(info);
    }

    @Override
    public OggMetadata loadMetadata(OggPacketInputStream stream, DirectBufferStreamBroker broker) throws IOException {
        FlacTrackInfo info = load(stream, broker);
        return new OggMetadata(info.tags, detectLength(info, stream));
    }

    private FlacTrackInfo load(OggPacketInputStream stream, DirectBufferStreamBroker broker) throws IOException {
        ByteBuffer buffer = broker.getBuffer();

        if (buffer.getInt(NATIVE_FLAC_HEADER_OFFSET) != NATIVE_FLAC_HEADER) {
            throw new IllegalStateException("Native flac header not found.");
        }

        buffer.position(NATIVE_FLAC_HEADER_OFFSET + 4);

        return readHeaders(buffer, stream);
    }

    private Long detectLength(FlacTrackInfo info, OggPacketInputStream stream) throws IOException {
        OggStreamSizeInfo sizeInfo;

        if (info.stream.sampleCount > 0) {
            sizeInfo = new OggStreamSizeInfo(0, info.stream.sampleCount, 0, 0, info.stream.sampleRate);
        } else {
            sizeInfo = stream.seekForSizeInfo(info.stream.sampleRate);
        }

        return sizeInfo != null ? sizeInfo.getDuration() : null;
    }

    private FlacTrackInfo readHeaders(ByteBuffer firstPacketBuffer, OggPacketInputStream packetInputStream) throws IOException {
        FlacStreamInfo streamInfo = FlacMetadataReader.readStreamInfoBlock(new DataInputStream(new ByteBufferInputStream(firstPacketBuffer)));
        FlacTrackInfoBuilder trackInfoBuilder = new FlacTrackInfoBuilder(streamInfo);

        DataInputStream dataInputStream = new DataInputStream(packetInputStream);

        boolean hasMoreMetadata = trackInfoBuilder.getStreamInfo().hasMetadataBlocks;

        while (hasMoreMetadata) {
            if (!packetInputStream.startNewPacket()) {
                throw new IllegalStateException("Track ended when more metadata was expected.");
            }

            hasMoreMetadata = FlacMetadataReader.readMetadataBlock(dataInputStream, packetInputStream, trackInfoBuilder);
        }

        return trackInfoBuilder.build();
    }

    private static class Blueprint implements OggTrackBlueprint {
        private final FlacTrackInfo info;

        private Blueprint(FlacTrackInfo info) {
            this.info = info;
        }

        @Override
        public OggTrackHandler loadTrackHandler(OggPacketInputStream stream) {
            return new OggFlacTrackHandler(info, stream);
        }

        @Override
        public int getSampleRate() {
            return info.stream.sampleRate;
        }
    }
}
