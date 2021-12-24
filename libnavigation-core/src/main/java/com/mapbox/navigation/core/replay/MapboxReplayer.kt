package com.mapbox.navigation.core.replay

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Context
import androidx.annotation.RequiresPermission
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.navigation.core.replay.history.ReplayEventBase
import com.mapbox.navigation.core.replay.history.ReplayEventSimulator
import com.mapbox.navigation.core.replay.history.ReplayEventUpdateLocation
import com.mapbox.navigation.core.replay.history.ReplayEvents
import com.mapbox.navigation.core.replay.history.ReplayEventsObserver
import com.mapbox.navigation.core.replay.route.ReplayRouteMapper
import java.util.Collections.singletonList

/**
 * This class is similar to a music player. It will include controls like play, pause, seek.
 */
class MapboxReplayer {

    private val replayEvents = ReplayEvents(mutableListOf())
    private val replayEventSimulator = ReplayEventSimulator(replayEvents)

    private val replayEventsObservers: MutableSet<ReplayEventsObserver> = mutableSetOf()

    /**
     * Appends events to be replayed. Notice the basis of your [ReplayEventBase.eventTimestamp].
     * When they are drastically different, you may need to [seekTo] events.
     *
     * @param events the events to be replayed.
     * @return [MapboxReplayer]
     */
    fun pushEvents(events: List<ReplayEventBase>): MapboxReplayer {
        this.replayEvents.events.addAll(events)
        return this
    }

    /**
     * Stops the player, seeks to the beginning, and clears all replay events. In order
     * to start playing a new route, [pushEvents] and then [play].
     */
    fun clearEvents() {
        stop()
        seekTo(0.0)
        replayEvents.events.clear()
    }

    /**
     * Register replay event observers.
     *
     * @param observer the observer registered
     */
    fun registerObserver(observer: ReplayEventsObserver) {
        replayEventsObservers.add(observer)
    }

    /**
     * Remove registered observers.
     *
     * @param observer the observer being removed
     */
    fun unregisterObserver(observer: ReplayEventsObserver) {
        replayEventsObservers.remove(observer)
    }

    /**
     * Remove all registered observers. If the player is still playing,
     * none of the events will be observed.
     *
     * @see [finish]
     */
    fun unregisterObservers() {
        replayEventsObservers.clear()
    }

    /**
     * This will begin playing the [ReplayEventBase] and notifying observers
     * registered via [registerObserver]
     */
    fun play() {
        replayEventSimulator.launchSimulator { replayEvents ->
            replayEventsObservers.forEach { it.replayEvents(replayEvents) }
        }
    }

    /**
     * Stop playing all remaining and incoming events. To play events, you must
     * restart the player by calling [play].
     *
     * @see [playbackSpeed] to pause the player
     * @see [finish] to clean up the player
     */
    fun stop() {
        replayEventSimulator.stopSimulator()
    }

    /**
     * This determines the speed of event playback. Default is 1.0 for 1x playback speed.
     *
     * For faster playback, use values greater than one such as 2x, 3x or even 4x.
     * For slower playback, use values between 0 and 1; 0.25 will replay at 1/4th speed.
     * To pause playback, use 0.0
     *
     * Negative (going backwards), is not supported. Use [seekTo] to go back in time.
     */
    fun playbackSpeed(scale: Double) {
        check(scale >= 0.0) { "Negative playback is not supported: $scale" }
        replayEventSimulator.playbackSpeed(scale)
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
            replayEventsObservers.forEach { it.replayEvents(replayEvents) }
        }
    }

    /**
     * When initializing or testing an app, it is needed and useful to push a device location.
     * This helper function can be used to push the actual location into the replayer.
     */
    @RequiresPermission(anyOf = [ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION])
    fun pushRealLocation(context: Context, eventTimestamp: Double) {
        LocationEngineProvider.getBestLocationEngine(context.applicationContext)
            .getLastLocation(
                object : LocationEngineCallback<LocationEngineResult> {
                    override fun onSuccess(result: LocationEngineResult?) {
                        result?.lastLocation?.let {
                            val event = ReplayRouteMapper.mapToUpdateLocation(eventTimestamp, it)
                            pushEvents(singletonList(event))
                        }
                    }

                    override fun onFailure(exception: Exception) {
                        // Intentionally empty
                    }
                }
            )
    }

    /**
     * The duration of the replay. This value will be between 0.0 and the total duration.
     *
     * @return the duration in seconds
     */
    fun durationSeconds(): Double {
        val firstEvent = replayEvents.events.firstOrNull()
            ?: return 0.0
        val lastEvent = replayEvents.events.last()
        return lastEvent.eventTimestamp - firstEvent.eventTimestamp
    }

    /**
     * The time of an event, relative to the duration of the replay.
     */
    fun eventSeconds(eventTimestamp: Double): Double {
        val firstEvent = replayEvents.events.firstOrNull()
            ?: return 0.0
        return eventTimestamp - firstEvent.eventTimestamp
    }

    /**
     * Seek to a time to play from.
     *
     * @param replayTime time in seconds between 0.0 to [durationSeconds]
     */
    fun seekTo(replayTime: Double) {
        val firstEventTime = replayEvents.events.firstOrNull()?.eventTimestamp
            ?: return
        val offsetTime = replayTime + firstEventTime
        val indexOfEvent = replayEvents.events
            .indexOfFirst { offsetTime <= it.eventTimestamp }
        check(indexOfEvent >= 0) {
            "Make sure your replayTime is less than replayDurationSeconds " +
                "$replayTime > ${durationSeconds()}: "
        }

        replayEventSimulator.seekTo(indexOfEvent)
    }

    /**
     * Seek to the event you want to play from.
     *
     * @param replayEvent an event that has been pushed to [pushEvents]
     * @throws IllegalStateException if [replayEvent] was not pushed
     */
    fun seekTo(replayEvent: ReplayEventBase) {
        val indexOfEvent = replayEvents.events.indexOf(replayEvent)
        check(indexOfEvent >= 0) { "You must first pushEvents and then seekTo an event" }

        replayEventSimulator.seekTo(indexOfEvent)
    }

    /**
     * Convenience function to stop, remove listeners, and clean up the player.
     */
    fun finish() {
        stop()
        unregisterObservers()
        clearEvents()
    }
}
