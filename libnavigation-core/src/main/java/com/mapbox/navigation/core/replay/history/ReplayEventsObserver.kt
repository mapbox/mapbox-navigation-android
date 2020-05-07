package com.mapbox.navigation.core.replay.history

/**
 * Used to observe events replayed by the [ReplayHistoryPlayer]
 */
interface ReplayEventsObserver {

    /**
     * Called with all events that occurred within a time window
     * during replay
     *
     * @param events events in chronological order, index 0 being the oldest
     */
    fun replayEvents(events: List<ReplayEventBase>)
}
