package com.mapbox.navigation.core.replay.history

import android.os.SystemClock
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.utils.internal.ThreadController
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToLong

/**
 * This class keeps track of a forward playing replay. As time moves forward, it captures
 * all events from [ReplayEvents] that happened, and provides them in a [ReplayEvents]
 *
 * @param replayEvents events needed to be replayed by [MapboxReplayer]
 */
internal class ReplayEventSimulator(
    private val replayEvents: ReplayEvents
) {

    private val jobControl = ThreadController.getMainScopeAndRootJob()

    // The pivot will move forward through the events with time.
    private var historyTimeOffset: Double = 0.0
    private var simulatorTimeOffset: Double = 0.0
    private var simulatorTimeScale: Double = 1.0

    private var pivotIndex = 0

    fun launchSimulator(replayEventsCallback: (List<ReplayEventBase>) -> Unit): Job {
        resetSimulatorClock()
        return jobControl.scope.launch {
            while (isActive) {
                if (isDonePlayingEvents()) {
                    delay(IS_DONE_PLAYING_EVENTS_DELAY_MILLIS)
                } else {
                    simulateEvents(replayEventsCallback)
                }
            }
        }
    }

    private suspend fun simulateEvents(replayEventsCallback: (List<ReplayEventBase>) -> Unit) {
        val loopStart = timeSeconds()

        val replayEvents = movePivot(loopStart)
        if (replayEvents.isNotEmpty()) {
            replayEventsCallback(replayEvents)
        }

        val loopElapsedSeconds = timeSeconds() - loopStart
        val loopElapsedMillis = (loopElapsedSeconds * MILLIS_PER_SECOND).roundToLong()
        val delayMillis = max(0L, REPLAY_UPDATE_SPEED_MILLIS - loopElapsedMillis)
        delay(delayMillis)
    }

    fun stopSimulator() {
        jobControl.job.cancelChildren()
    }

    fun seekTo(indexOfEvent: Int) {
        historyTimeOffset = replayEvents.events[indexOfEvent].eventTimestamp
        pivotIndex = indexOfEvent
        resetSimulatorClock()
    }

    fun playbackSpeed(scale: Double) {
        simulatorTimeScale = scale
        resetSimulatorClock()
    }

    private fun resetSimulatorClock() {
        simulatorTimeOffset = timeSeconds()
        historyTimeOffset = if (isDonePlayingEvents()) {
            replayEvents.events.lastOrNull()?.eventTimestamp ?: 0.0
        } else {
            replayEvents.events[pivotIndex].eventTimestamp
        }
    }

    private fun movePivot(timeSeconds: Double): List<ReplayEventBase> {
        val simulatorTime = (timeSeconds - simulatorTimeOffset)
        check(simulatorTime >= 0) { "Simulator can only move forward in time" }

        val eventHappened = mutableListOf<ReplayEventBase>()
        for (i in pivotIndex until replayEvents.events.size) {
            val event = replayEvents.events[pivotIndex]
            val eventTime = event.eventTimestamp - historyTimeOffset
            if (eventTime <= simulatorTime) {
                eventHappened.add(event)
                pivotIndex++
            } else {
                break
            }
        }

        return eventHappened
    }

    private fun isDonePlayingEvents(): Boolean {
        return pivotIndex >= replayEvents.events.size
    }

    private fun timeSeconds(): Double {
        val elapsedNanos = SystemClock.elapsedRealtimeNanos().toDouble() * NANOS_PER_SECOND
        return elapsedNanos * simulatorTimeScale
    }

    companion object {

        // The frequency that replay updates will be broad-casted
        private const val REPLAY_UPDATE_SPEED_MILLIS = 100L

        // When there are no events to play, delay the coroutine
        private const val IS_DONE_PLAYING_EVENTS_DELAY_MILLIS = 1000L

        private const val MILLIS_PER_SECOND = 1000
        private const val NANOS_PER_SECOND = 1e-9
    }
}
