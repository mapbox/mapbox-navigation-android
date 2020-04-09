package com.mapbox.navigation.core.replay.history

/**
 * This class keeps track of a forward playing replay. As time moves forward, it captures
 * all events from [ReplayEvents] that happened, and provides them in a [ReplayEvents]
 */
internal class ReplayEventLookup(
    private val replayEvents: ReplayEvents
) {
    private val numEvents = replayEvents.events.size

    // The pivot will move forward through the events with time.
    private var historyTimeOffset: Double = 0.0
    private var simulatorTimeOffset: Double = 0.0

    private var pivotIndex = 0

    fun initPivot(timeSeconds: Double) {
        simulatorTimeOffset = timeSeconds
        historyTimeOffset = replayEvents.events[pivotIndex].eventTimestamp
    }

    fun movePivot(timeSeconds: Double): ReplayEvents {
        val simulatorTime = (timeSeconds - simulatorTimeOffset)
        if (simulatorTime < 0.01) return ReplayEvents(emptyList())
        check(simulatorTime >= 0) { "Rewind is not supported yet" }

        val eventHappened = mutableListOf<ReplayEventBase>()
        for (i in pivotIndex until numEvents) {
            val event = replayEvents.events[pivotIndex]
            val eventTime = event.eventTimestamp - historyTimeOffset
            if (eventTime <= simulatorTime) {
                eventHappened.add(event)
                pivotIndex++
            } else {
                break
            }
        }

        return ReplayEvents(eventHappened)
    }

    fun isComplete(): Boolean {
        return pivotIndex >= replayEvents.events.size
    }
}
