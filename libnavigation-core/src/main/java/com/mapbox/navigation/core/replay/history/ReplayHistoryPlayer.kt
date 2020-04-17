package com.mapbox.navigation.core.replay.history

import androidx.lifecycle.LifecycleOwner
import com.mapbox.android.core.location.LocationEngine
import java.util.Collections.singletonList
import kotlinx.coroutines.Job

typealias ReplayEventsListener = (List<ReplayEventBase>) -> Unit

/**
 * This class is similar to a music player. It will include controls like play, pause, seek.
 */
class ReplayHistoryPlayer {
    private val replayEvents = ReplayEvents(mutableListOf())
    private val replayEventSimulator = ReplayEventSimulator(replayEvents)

    private val replayEventsListeners: MutableList<ReplayEventsListener> = mutableListOf()

    /**
     * Appends events to be replayed.
     */
    fun pushEvents(events: List<ReplayEventBase>): ReplayHistoryPlayer {
        this.replayEvents.events.addAll(events)
        return this
    }

    /**
     * Events from the [ReplayEvents] will be published to your listener.
     * Your subscriber will be removed after you call [finish]
     */
    fun observeReplayEvents(function: ReplayEventsListener) {
        replayEventsListeners.add(function)
    }

    /**
     * The duration of the replay. This value will be between 0.0 and the total duration.
     *
     * @return the duration in seconds
     */
    fun replayDurationSeconds(): Double {
        val firstEvent = replayEvents.events.first()
        val lastEvent = replayEvents.events.last()
        return lastEvent.eventTimestamp - firstEvent.eventTimestamp
    }

    /**
     * Meant to be called from an Android component, such as an Activity, Fragment, or ViewModel.
     * This will begin playing the [ReplayEvents] and notifying listeners attached to
     * [observeReplayEvents]
     */
    fun play(lifecycleOwner: LifecycleOwner): Job {
        return replayEventSimulator.launchPlayLoop(lifecycleOwner) { replayEvents ->
            replayEventsListeners.forEach { it(replayEvents) }
        }
    }

    /**
     * When initializing or testing an app, it is needed and useful to decide a code location
     * to play the first location from GPS. When it is early, you may crash. When it
     * is late, you may start in null island - LatLng(0,0).
     * Use this function to play the first location received from your [LocationEngine].
     */
    fun playFirstLocation() {
        val firstUpdateLocation = replayEvents.events.firstOrNull { replayEvent ->
            replayEvent is ReplayEventUpdateLocation
        }
        firstUpdateLocation?.let { replayEvent ->
            val replayEvents = singletonList(replayEvent)
            replayEventsListeners.forEach { it(replayEvents) }
        }
    }

    /**
     * Seek to a time to play from.
     *
     * @param replayTime time in seconds between 0.0 to [replayDurationSeconds]
     */
    fun seekTo(replayTime: Double) {
        val offsetTime = replayTime + replayEvents.events.first().eventTimestamp
        val indexOfEvent = replayEvents.events
            .indexOfFirst { offsetTime <= it.eventTimestamp }
        check(indexOfEvent >= 0) { "Make sure your replayTime is less than replayDurationSeconds $replayTime > ${replayDurationSeconds()}: " }

        replayEventSimulator.seekTo(indexOfEvent)
    }

    /**
     * Seek to the event you want to play from.
     */
    fun seekTo(replayEvent: ReplayEventBase) {
        val indexOfEvent = replayEvents.events.indexOf(replayEvent)
        check(indexOfEvent >= 0) { "You must first pushEvents and then seekTo an event" }

        replayEventSimulator.seekTo(indexOfEvent)
    }

    /**
     * Remove all the [observeReplayEvents]. Designed to be called in tear down functions like
     * Activity.onDestroy, Fragment.onDestroy, or ViewModel.onCleared
     */
    fun finish() {
        replayEventSimulator.stopPlaying()
        replayEventsListeners.clear()
    }
}
