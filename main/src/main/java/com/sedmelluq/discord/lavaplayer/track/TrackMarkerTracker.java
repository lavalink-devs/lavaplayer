package com.sedmelluq.discord.lavaplayer.track;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import static com.sedmelluq.discord.lavaplayer.track.TrackMarkerHandler.MarkerState.*;

/**
 * Tracks the state of a track position marker.
 */
public class TrackMarkerTracker {
    private final ConcurrentLinkedQueue<TrackMarker> markerQueue = new ConcurrentLinkedQueue<>();

    /**
     * Set a new track position marker. This removes all previously set markers.
     *
     * @param marker          Marker
     * @param currentTimecode Current timecode of the track when this marker is set
     */
    public void set(TrackMarker marker, long currentTimecode) {
        markerQueue.forEach(this::remove);

        add(marker, currentTimecode);
    }

    public void add(TrackMarker marker, long currentTimecode) {
        final List<TrackMarker> previousMarkers = markerQueue.stream()
            .filter(marker1 -> marker1.timecode == currentTimecode)
            .collect(Collectors.toList());
        final TrackMarkerHandler.MarkerState handleState = marker != null ? OVERWRITTEN : REMOVED;

        for (TrackMarker previous : previousMarkers) {
            trigger(previous, handleState);
        }

        if (marker != null) {
            markerQueue.offer(marker);

            if (currentTimecode >= marker.timecode) {
                trigger(marker, LATE);
            }
        }
    }

    public void remove(TrackMarker marker) {
        trigger(marker, REMOVED);
    }

    /**
     * Removes the first marker in the queue.
     *
     * @return The removed marker.
     */
    @Deprecated
    public TrackMarker remove() {
        return markerQueue.poll();
    }

    /**
     * Trigger and remove the marker with the specified state.
     *
     * @param state The state of the marker to pass to the handler.
     */
    public void trigger(TrackMarkerHandler.MarkerState state) {
        for (TrackMarker marker : markerQueue) {
            marker.handler.handle(state);
        }
    }

    /**
     * Check a timecode which was reached by normal playback, trigger REACHED if necessary.
     *
     * @param timecode Timecode which was reached by normal playback.
     */
    public void checkPlaybackTimecode(long timecode) {
        for (TrackMarker marker : markerQueue) {
            if (marker != null && timecode >= marker.timecode) {
                trigger(marker, REACHED);
            }
        }
    }

    /**
     * Check a timecode which was reached by seeking, trigger BYPASSED if necessary.
     *
     * @param timecode Timecode which was reached by seeking.
     */
    public void checkSeekTimecode(long timecode) {
        for (TrackMarker marker : markerQueue) {
            if (marker != null && timecode >= marker.timecode) {
                trigger(marker, BYPASSED);
            }
        }
    }

    private void trigger(TrackMarker marker, TrackMarkerHandler.MarkerState state) {
        if (markerQueue.remove(marker)) {
            marker.handler.handle(state);
        }
    }
}
