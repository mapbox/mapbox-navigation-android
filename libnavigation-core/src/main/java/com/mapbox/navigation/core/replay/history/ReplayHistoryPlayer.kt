package com.mapbox.navigation.core.replay.history

import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.mapbox.navigation.utils.thread.ThreadController
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

typealias ReplayEventsListener = (ReplayEvents) -> Unit

/**
 * This class is similar to a music player. It will include controls like play, pause, seek.
 */
class ReplayHistoryPlayer(
    replayEvents: ReplayEvents
) {
    private val replayEventLookup = ReplayEventLookup(replayEvents)

    private val replayEventsListeners: MutableList<ReplayEventsListener> = mutableListOf()
    private val jobControl = ThreadController.getMainScopeAndRootJob()

    /**
     * Events from the [ReplayEvents] will be published to your listener.
     * Your subscriber will be removed after you call [finish]
     */
    fun observeReplayEvents(function: ReplayEventsListener) {
        replayEventsListeners.add(function)
    }

    /**
     * Meant to be called from an Android component, such as an Activity, Fragment, or ViewModel.
     * This will begin playing the [ReplayEvents] and notifying listeners attached to
     * [observeReplayEvents]
     */
    fun play(lifecycleOwner: LifecycleOwner): Job {
        Log.i("ReplayHistory", "Simulator started")

        replayEventLookup.initPivot(timeSeconds())

        return jobControl.scope.launch {
            while (isActive && isSimulating(lifecycleOwner)) {
                val loopStart = timeSeconds()

                val recordUpdate = replayEventLookup.movePivot(timeSeconds())
                replayEventsListeners.forEach { it.invoke(recordUpdate) }

                val loopElapsed = ((timeSeconds() - loopStart) * MILLIS_PER_SECOND).roundToInt()
                val delayMillis = abs(replayUpdateSpeedMillis - loopElapsed)
                delay(delayMillis)
            }

            Log.i("ReplayHistory", "Simulator ended")
        }
    }

    /**
     * Remove all the [observeReplayEvents]. Designed to be called in tear down functions like
     * Activity.onDestroy, Fragment.onDestroy, or ViewModel.onCleared
     */
    fun finish() {
        jobControl.job.cancelChildren()
        replayEventsListeners.clear()
    }

    private fun isSimulating(lifecycleOwner: LifecycleOwner): Boolean {
        return lifecycleOwner.lifecycle.currentState != Lifecycle.State.DESTROYED &&
            !replayEventLookup.isComplete()
    }

    companion object {
        // The frequency that replay updates will be broad-casted
        private const val replayUpdateSpeedMillis = 1000L

        private const val MILLIS_PER_SECOND = 1e+4
        private const val NANOS_PER_SECOND = 1e-9
        private fun timeSeconds(): Double = SystemClock.elapsedRealtimeNanos().toDouble() * NANOS_PER_SECOND
    }
}
