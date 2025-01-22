package com.mapbox.navigation.core.replay.history

import com.mapbox.navigation.core.replay.MapboxReplayer

/**
 * Used to observe events replayed by the [MapboxReplayer]
 */
fun interface ReplayEventsObserver {

    /**
     * Called with all events that occurred within a time window
     * during replay
     *
     * @param events events in chronological order, index 0 being the oldest
     */
    fun replayEvents(events: List<ReplayEventBase>)
}
