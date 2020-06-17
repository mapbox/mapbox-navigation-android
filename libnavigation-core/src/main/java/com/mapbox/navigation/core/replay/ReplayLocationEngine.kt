package com.mapbox.navigation.core.replay

import android.app.PendingIntent
import android.location.Location
import android.os.Looper
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.navigation.core.replay.history.ReplayEventBase
import com.mapbox.navigation.core.replay.history.ReplayEventUpdateLocation
import com.mapbox.navigation.core.replay.history.ReplayEventsObserver
import java.util.Date

private typealias EngineCallback = LocationEngineCallback<LocationEngineResult>

/**
 * Location Engine for replaying route history.
 */
class ReplayLocationEngine : LocationEngine, ReplayEventsObserver {

    private val registeredCallbacks: MutableList<EngineCallback> = mutableListOf()
    private val lastLocationCallbacks: MutableList<EngineCallback> = mutableListOf()
    private var lastLocationEngineResult: LocationEngineResult? = null

    /**
     * Requests location updates with a callback on the specified Looper thread.
     */
    override fun requestLocationUpdates(request: LocationEngineRequest, callback: EngineCallback, looper: Looper?) {
        registeredCallbacks.add(callback)
    }

    /**
     * Removes location updates for the given location engine callback.
     *
     * It is recommended to remove location requests when the activity is in a paused or
     * stopped state, doing so helps battery performance.
     */
    override fun removeLocationUpdates(callback: EngineCallback) {
        registeredCallbacks.remove(callback)
    }

    /**
     * Returns the most recent location currently available.
     *
     * If a location is not available, which should happen very rarely, null will be returned.
     */
    override fun getLastLocation(callback: EngineCallback) {
        if (lastLocationEngineResult != null) {
            callback.onSuccess(lastLocationEngineResult)
        } else {
            lastLocationCallbacks.add(callback)
        }
    }

    /**
     * Requests location updates with callback on the specified PendingIntent.
     */
    override fun requestLocationUpdates(request: LocationEngineRequest, pendingIntent: PendingIntent?) {
        throw UnsupportedOperationException("requestLocationUpdates with intents is unsupported")
    }

    /**
     * Removes location updates for the given pending intent.
     *
     * It is recommended to remove location requests when the activity is in a paused or
     * stopped state, doing so helps battery performance.
     */
    override fun removeLocationUpdates(pendingIntent: PendingIntent?) {
        throw UnsupportedOperationException("removeLocationUpdates with intents is unsupported")
    }

    override fun replayEvents(replayEvents: List<ReplayEventBase>) {
        replayEvents.forEach { event ->
            when (event) {
                is ReplayEventUpdateLocation -> replayLocation(event)
            }
        }
    }

    private fun replayLocation(event: ReplayEventUpdateLocation) {
        val eventLocation = event.location
        val location = Location(eventLocation.provider)
        location.longitude = eventLocation.lon
        location.latitude = eventLocation.lat
        location.time = Date().time
        eventLocation.accuracyHorizontal?.toFloat()?.let { location.accuracy = it }
        eventLocation.bearing?.toFloat()?.let { location.bearing = it }
        eventLocation.altitude?.let { location.altitude = it }
        eventLocation.speed?.toFloat()?.let { location.speed = it }
        val locationEngineResult = LocationEngineResult.create(location)
        lastLocationEngineResult = locationEngineResult

        registeredCallbacks.forEach { it.onSuccess(locationEngineResult) }
        lastLocationCallbacks.forEach { it.onSuccess(locationEngineResult) }
        lastLocationCallbacks.clear()
    }
}
