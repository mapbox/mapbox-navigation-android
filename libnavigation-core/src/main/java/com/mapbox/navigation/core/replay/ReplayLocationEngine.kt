package com.mapbox.navigation.core.replay

import androidx.annotation.UiThread
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.bindgen.Value
import com.mapbox.common.location.GetLocationCallback
import com.mapbox.common.location.LiveTrackingClient
import com.mapbox.common.location.LiveTrackingClientObserver
import com.mapbox.common.location.LiveTrackingState
import com.mapbox.common.location.Location
import com.mapbox.common.location.LocationClientStartStopCallback
import com.mapbox.common.location.LocationError
import com.mapbox.navigation.core.internal.location.toCommonLocation
import com.mapbox.navigation.core.replay.history.ReplayEventBase
import com.mapbox.navigation.core.replay.history.ReplayEventUpdateLocation
import com.mapbox.navigation.core.replay.history.ReplayEventsObserver
import com.mapbox.navigation.core.replay.history.mapToLocation
import java.util.concurrent.CopyOnWriteArrayList

private typealias EngineCallback = LocationEngineCallback<LocationEngineResult>

/**
 * Location Engine for replaying route history.
 */
@UiThread
class ReplayLocationEngine(
    private val mapboxReplayer: MapboxReplayer
) : LiveTrackingClient, ReplayEventsObserver {

    private val registeredCallbacks: MutableList<LiveTrackingClientObserver> =
        CopyOnWriteArrayList()
    private val lastLocationCallbacks: MutableList<GetLocationCallback> = mutableListOf()
    private var lastLocationEngineResult: Expected<LocationError, Location>? = null

    init {
        mapboxReplayer.registerObserver(this)
    }

    override fun start(settings: Value?, callback: LocationClientStartStopCallback) {
        TODO("Not yet implemented")
    }

    override fun stop(callback: LocationClientStartStopCallback) {
        TODO("Not yet implemented")
    }

    override fun registerObserver(observer: LiveTrackingClientObserver) {
        registeredCallbacks.add(observer)
    }

    override fun unregisterObserver(observer: LiveTrackingClientObserver) {
        registeredCallbacks.remove(observer)
    }

    override fun getName(): String = "ReplayLocationEngine"

    // TODO: Review if always started makes sense or if it should only be STARTED if we're replaying
    override fun getState(): LiveTrackingState = LiveTrackingState.STARTED

    override fun getActiveSettings(): Value? {
        TODO("Not yet implemented")
    }

    override fun flush() {
        // NO-OP
    }

    /**
     * Requests location updates with a callback on the specified Looper thread.
     */
//    override fun requestLocationUpdates(
//        request: LocationEngineRequest,
//        callback: EngineCallback,
//        looper: Looper?
//    ) {
//        registeredCallbacks.add(callback)
//    }

    /**
     * Removes location updates for the given location engine callback.
     *
     * It is recommended to remove location requests when the activity is in a paused or
     * stopped state, doing so helps battery performance.
     */
//    override fun removeLocationUpdates(callback: EngineCallback) {
//        registeredCallbacks.remove(callback)
//    }

    /**
     * Returns the most recent location currently available.
     *
     * If a location is not available, which should happen very rarely, null will be returned.
     */
    /*override*/ fun getLastLocation(callback: GetLocationCallback) {
        lastLocationEngineResult?.let {
            callback.run(it)
        } ?: lastLocationCallbacks.add(callback)
    }

    /**
     * Requests location updates with callback on the specified PendingIntent.
     */
//    override fun requestLocationUpdates(
//        request: LocationEngineRequest,
//        pendingIntent: PendingIntent?
//    ) {
//        throw UnsupportedOperationException("requestLocationUpdates with intents is unsupported")
//    }

    /**
     * Removes location updates for the given pending intent.
     *
     * It is recommended to remove location requests when the activity is in a paused or
     * stopped state, doing so helps battery performance.
     */
//    override fun removeLocationUpdates(pendingIntent: PendingIntent?) {
//        throw UnsupportedOperationException("removeLocationUpdates with intents is unsupported")
//    }

    override fun replayEvents(events: List<ReplayEventBase>) {
        events.forEach { replayEventBase ->
            when (replayEventBase) {
                is ReplayEventUpdateLocation -> replayLocation(replayEventBase)
            }
        }
    }

    internal fun cleanUpLastLocation() {
        lastLocationEngineResult = null
    }

    private fun replayLocation(event: ReplayEventUpdateLocation) {
        val eventLocation = event.location
        val location = eventLocation.mapToLocation(
            eventTimeOffset = mapboxReplayer.eventRealtimeOffset(event.eventTimestamp)
        )
        val commonLocation = location.toCommonLocation()
        val locationEngineResult: Expected<LocationError, Location> =
            ExpectedFactory.createValue(commonLocation)
        lastLocationEngineResult = locationEngineResult

        val locationUpdate: Expected<LocationError, List<Location>> =
            ExpectedFactory.createValue(listOf(commonLocation))

        registeredCallbacks.forEach { it.onLocationUpdateReceived(locationUpdate) }
        lastLocationCallbacks.forEach { it.run(locationEngineResult) }
        lastLocationCallbacks.clear()
    }
}
