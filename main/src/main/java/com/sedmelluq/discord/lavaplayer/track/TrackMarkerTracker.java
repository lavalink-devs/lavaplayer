package com.sedmelluq.discord.lavaplayer.track;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.sedmelluq.discord.lavaplayer.track.TrackMarkerHandler.MarkerState.BYPASSED;
import static com.sedmelluq.discord.lavaplayer.track.TrackMarkerHandler.MarkerState.LATE;
import static com.sedmelluq.discord.lavaplayer.track.TrackMarkerHandler.MarkerState.REACHED;
import static com.sedmelluq.discord.lavaplayer.track.TrackMarkerHandler.MarkerState.REMOVED;

/**
 * Tracks the state of a track position marker.
 */
public class TrackMarkerTracker {
    private final List<TrackMarker> markerList = new CopyOnWriteArrayList<>();

    /**
     * Set a new track position marker. This removes all previously set markers.
     *
     * @param marker          Marker
     * @param currentTimecode Current timecode of the track when this marker is set
     */
    public void set(TrackMarker marker, long currentTimecode) {
        markerList.forEach(this::remove);

        add(marker, currentTimecode);
    }

    public void add(TrackMarker marker, long currentTimecode) {
        if (marker != null) {
            markerList.add(marker);

            if (currentTimecode >= marker.timecode) {
                trigger(marker, LATE);
            }
        }
    }

    public void remove(TrackMarker marker) {
        trigger(marker, REMOVED);
    }

    /**
     * Removes the first marker in the list.
     *
     * @return The removed marker. Null if there are no markers.
     */
    @Deprecated
    public TrackMarker remove() {
        if (markerList.isEmpty()) {
            return null;
        }

        return markerList.remove(0);
    }

    public List<TrackMarker> getMarkers() {
        return markerList;
    }

    public void clear() {
        markerList.clear();
    }

    /**
     * Trigger and remove the marker with the specified state.
     *
     * @param state The state of the marker to pass to the handler.
     */
    public void trigger(TrackMarkerHandler.MarkerState state) {
        for (TrackMarker marker : markerList) {
            marker.handler.handle(state);
        }
    }

    /**
     * Check a timecode which was reached by normal playback, trigger REACHED if necessary.
     *
     * @param timecode Timecode which was reached by normal playback.
     */
    public void checkPlaybackTimecode(long timecode) {
        for (TrackMarker marker : markerList) {
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
        for (TrackMarker marker : markerList) {
            if (marker != null && timecode >= marker.timecode) {
                trigger(marker, BYPASSED);
            }
        }
    }

    private void trigger(TrackMarker marker, TrackMarkerHandler.MarkerState state) {
        if (markerList.remove(marker)) {
            marker.handler.handle(state);
        }
    }
}
