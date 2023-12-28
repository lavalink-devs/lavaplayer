package com.sedmelluq.discord.lavaplayer.track;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.sedmelluq.discord.lavaplayer.track.TrackMarkerHandler.MarkerState.*;

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
        if (marker == null) {
            trigger(REMOVED);
        } else {
            trigger(OVERWRITTEN);

            add(marker, currentTimecode);
        }
    }

    public void add(TrackMarker marker, long currentTimecode) {
        if (marker != null) {
            if (currentTimecode >= marker.timecode) {
                marker.handler.handle(LATE);
            } else {
                markerList.add(marker);
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
     *
     * @deprecated Use {@link #getMarkers()} and {@link #clear()} instead.
     */
    @Deprecated
    public TrackMarker remove() {
        if (markerList.isEmpty()) {
            return null;
        }

        return markerList.remove(0);
    }

    /**
     * @return The current unmodifiable list of timecode markers stored in this tracker.
     * @see #add(TrackMarker, long)
     * @see #remove(TrackMarker)
     * @see #clear()
     */
    public List<TrackMarker> getMarkers() {
        return Collections.unmodifiableList(markerList);
    }

    public void clear() {
        markerList.clear();
    }

    /**
     * Triggers and removes all markers with the specified state.
     *
     * @param state The state of the marker to pass to the handler.
     */
    public void trigger(TrackMarkerHandler.MarkerState state) {
        for (TrackMarker marker : markerList) {
            marker.handler.handle(state);
        }

        this.clear();
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
