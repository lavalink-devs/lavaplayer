package com.sedmelluq.discord.lavaplayer.container.ogg;

public class OggSeekPoint {
    private final long position;
    private final long granulePosition;
    private final long timecode;
    private final long pageSequence;

    /**
     * @param position        The position of the seek point in the stream, in bytes.
     * @param granulePosition The granule position of the seek point in the stream.
     * @param timecode        The time of the seek point in the stream, in milliseconds.
     * @param pageSequence    The page to what this seek point belong.
     */
    public OggSeekPoint(long position, long granulePosition, long timecode, long pageSequence) {
        this.position = position;
        this.granulePosition = granulePosition;
        this.timecode = timecode;
        this.pageSequence = pageSequence;
    }

    /**
     * @return The position of the seek point in the stream, in bytes.
     */
    public long getPosition() {
        return position;
    }

    /**
     * @return The granule position of the seek point in the stream.
     */
    public long getGranulePosition() {
        return granulePosition;
    }

    /**
     * @return The timecode of the seek point in the stream, in milliseconds.
     */
    public long getTimecode() {
        return timecode;
    }

    /**
     * @return The page to what this seek point belong.
     */
    public long getPageSequence() {
        return pageSequence;
    }
}
