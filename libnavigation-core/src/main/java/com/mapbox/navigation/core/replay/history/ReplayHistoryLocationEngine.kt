package com.mapbox.navigation.core.replay.history

import android.app.PendingIntent
import android.location.Location
import android.os.Looper
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.android.core.location.LocationEngineResult

private typealias EngineCallback = LocationEngineCallback<LocationEngineResult>

class ReplayHistoryLocationEngine(
    replayHistoryPlayer: ReplayHistoryPlayer
) : LocationEngine {

    private val registeredCallbacks: MutableList<EngineCallback> = mutableListOf()
    private val lastLocationCallbacks: MutableList<EngineCallback> = mutableListOf()
    private var lastLocationEngineResult: LocationEngineResult? = null
    private val myId: Int

    companion object {
        var instances = 0
            get() { return field++ }
    }

    init {
        myId = instances
        replayHistoryPlayer.observeReplayEvents { recordUpdate ->
            replayEvents(recordUpdate)
        }
    }

    override fun requestLocationUpdates(request: LocationEngineRequest, callback: EngineCallback, looper: Looper?) {
        registeredCallbacks.add(callback)
    }

    override fun removeLocationUpdates(callback: EngineCallback) {
        registeredCallbacks.remove(callback)
    }

    override fun getLastLocation(callback: EngineCallback) {
        if (lastLocationEngineResult != null) {
            callback.onSuccess(lastLocationEngineResult)
        } else {
            lastLocationCallbacks.add(callback)
        }
    }

    override fun requestLocationUpdates(request: LocationEngineRequest, pendingIntent: PendingIntent?) {
        throw UnsupportedOperationException("$myId requestLocationUpdates with intents is unsupported")
    }

    override fun removeLocationUpdates(pendingIntent: PendingIntent?) {
        throw UnsupportedOperationException("$myId removeLocationUpdates with intents is unsupported")
    }

    private fun replayEvents(replayEvents: ReplayEvents) {
        replayEvents.events.forEach { event ->
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
        location.time = System.currentTimeMillis()
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
