package com.mapbox.navigation.core.replay.history

import android.os.SystemClock
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.mapbox.base.common.logger.Logger
import com.mapbox.base.common.logger.model.Message
import com.mapbox.navigation.utils.thread.ThreadController
import java.lang.IllegalStateException
import kotlin.math.max
import kotlin.math.roundToLong
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * This class keeps track of a forward playing replay. As time moves forward, it captures
 * all events from [ReplayEvents] that happened, and provides them in a [ReplayEvents]
 *
 * @param replayEvents events needed to be replayed by [ReplayHistoryPlayer]
 * @param logger interface for logging any events
 */
internal class ReplayEventSimulator(
    private val replayEvents: ReplayEvents,
    private val logger: Logger
) {

    private val jobControl = ThreadController.getMainScopeAndRootJob()

    // The pivot will move forward through the events with time.
    private var historyTimeOffset: Double = 0.0
    private var simulatorTimeOffset: Double = 0.0

    private var pivotIndex = 0

    fun seekTo(indexOfEvent: Int) {
        historyTimeOffset = replayEvents.events[indexOfEvent].eventTimestamp
        pivotIndex = indexOfEvent
        resetSimulatorClock()
    }

    fun launchPlayLoop(lifecycleOwner: LifecycleOwner, replayEventsCallback: (List<ReplayEventBase>) -> Unit): Job {
        logger.i(msg = Message("Replay started"))
        resetSimulatorClock()
        return jobControl.scope.launch {
            while (isActive && isSimulating(lifecycleOwner)) {
                val loopStart = timeSeconds()

                val replayEvents = movePivot(loopStart)
                if (replayEvents.isNotEmpty()) {
                    replayEventsCallback(replayEvents)
                }

                val loopElapsedSeconds = timeSeconds() - loopStart
                val loopElapsedMillis = (loopElapsedSeconds * MILLIS_PER_SECOND).roundToLong()
                val delayMillis = max(0L, replayUpdateSpeedMillis - loopElapsedMillis)
                delay(delayMillis)
            }

            if (lifecycleOwner.lifecycle.currentState == Lifecycle.State.DESTROYED) {
                throw IllegalStateException("Make sure to call ReplayHistoryPlayer.finish()")
            }

            logger.i(msg = Message("Replay ended"))
        }
    }

    fun stopPlaying() {
        jobControl.job.cancelChildren()
    }

    private fun resetSimulatorClock() {
        simulatorTimeOffset = timeSeconds()
        historyTimeOffset = replayEvents.events[pivotIndex].eventTimestamp
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

    private fun isSimulating(lifecycleOwner: LifecycleOwner): Boolean {
        return lifecycleOwner.lifecycle.currentState != Lifecycle.State.DESTROYED &&
            !isComplete()
    }

    private fun isComplete(): Boolean {
        return pivotIndex >= replayEvents.events.size
    }

    companion object {

        // The frequency that replay updates will be broad-casted
        private const val replayUpdateSpeedMillis = 100L

        private const val MILLIS_PER_SECOND = 1000
        private const val NANOS_PER_SECOND = 1e-9
        private fun timeSeconds(): Double = SystemClock.elapsedRealtimeNanos().toDouble() * NANOS_PER_SECOND
    }
}
