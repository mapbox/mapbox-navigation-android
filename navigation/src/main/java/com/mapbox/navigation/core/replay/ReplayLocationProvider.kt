package com.mapbox.navigation.core.replay

import android.os.Handler
import android.os.Looper
import androidx.annotation.UiThread
import com.mapbox.common.Cancelable
import com.mapbox.common.location.GetLocationCallback
import com.mapbox.common.location.Location
import com.mapbox.common.location.LocationObserver
import com.mapbox.common.location.LocationProvider
import com.mapbox.navigation.core.replay.history.ReplayEventBase
import com.mapbox.navigation.core.replay.history.ReplayEventUpdateLocation
import com.mapbox.navigation.core.replay.history.ReplayEventsObserver
import com.mapbox.navigation.core.replay.history.mapToLocation

/**
 * Location Provider for replaying route history.
 */
@UiThread
class ReplayLocationProvider(
    private val mapboxReplayer: MapboxReplayer,
) : LocationProvider, ReplayEventsObserver {

    private val locationObservers: MutableMap<LocationObserver, Looper?> = linkedMapOf()
    private val lastLocationCallbacks: MutableList<GetLocationCallback> = mutableListOf()
    private var lastLocation: Location? = null

    init {
        mapboxReplayer.registerObserver(this)
    }

    /**
     * Registers an observer in this instance of LocationProvider.
     * One instance of LocationProvider can have more than one observer.
     * If you add the same observer twice, it will only be invoked once.
     * @param observer an observer to add
     */
    override fun addLocationObserver(observer: LocationObserver) {
        locationObservers[observer] = null
    }

    /**
     * Registers an observer that will be invoked on a specific looper
     * in this instance of LocationProvider.
     * One instance of LocationProvider can have more than one observer.
     * If you add the same observer twice (even with different loopers or if the looper wasn't set),
     * it will only be invoked once on the latest looper that was passed.
     * @param observer an observer to add
     * @param looper the looper the observer will be invoked on
     */
    override fun addLocationObserver(observer: LocationObserver, looper: Looper) {
        locationObservers[observer] = looper
    }

    /**
     * Gets the last known location.
     * This call will never activate hardware to obtain a new location, and will only return a cached location.
     * @param callback a callback to return the last known location from a cache or a error if it fails.
     * @return cancelable object to stop callback from being invoked
     */
    override fun getLastLocation(callback: GetLocationCallback): Cancelable {
        if (lastLocation != null) {
            callback.run(lastLocation)
        } else {
            lastLocationCallbacks.add(callback)
        }
        return Cancelable { lastLocationCallbacks.remove(callback) }
    }

    /**
     * Removes the observer from this instance of LocationProvider.
     * If the observer is not registered, this is no-op.
     * @param observer an observer to remove
     */
    override fun removeLocationObserver(observer: LocationObserver) {
        locationObservers.remove(observer)
    }

    override fun replayEvents(replayEvents: List<ReplayEventBase>) {
        replayEvents.forEach { replayEventBase ->
            when (replayEventBase) {
                is ReplayEventUpdateLocation -> replayLocation(replayEventBase)
            }
        }
    }

    internal fun cleanUpLastLocation() {
        lastLocation = null
    }

    private fun replayLocation(event: ReplayEventUpdateLocation) {
        val eventLocation = event.location
        val location = eventLocation.mapToLocation(
            eventTimeOffset = mapboxReplayer.eventRealtimeOffset(event.eventTimestamp),
        )
        lastLocation = location

        // to avoid ConcurrentModificationException
        val entriesCache = locationObservers.entries.toList()
        entriesCache.forEach { (observer, looper) ->
            notifyObserver(observer, looper, location)
        }
        lastLocationCallbacks.forEach { it.run(location) }
        lastLocationCallbacks.clear()
    }

    private fun notifyObserver(observer: LocationObserver, looper: Looper?, location: Location) {
        val locations = listOf(location)
        if (looper == null || Looper.myLooper() == looper) {
            observer.onLocationUpdateReceived(locations)
        } else {
            Handler(looper).post { observer.onLocationUpdateReceived(locations) }
        }
    }
}
