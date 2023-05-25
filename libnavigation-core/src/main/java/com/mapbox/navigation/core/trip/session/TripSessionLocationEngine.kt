package com.mapbox.navigation.core.trip.session

import android.annotation.SuppressLint
import android.location.Location
import android.os.SystemClock
import com.mapbox.bindgen.Expected
import com.mapbox.common.location.LiveTrackingClientObserver
import com.mapbox.common.location.LiveTrackingState
import com.mapbox.common.location.LocationError
import com.mapbox.common.location.LocationServiceFactory
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.internal.location.NavLocationService
import com.mapbox.navigation.core.internal.location.toAndroidLocation
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.utils.internal.logD
import com.mapbox.navigation.utils.internal.logW
import java.util.concurrent.TimeUnit

/**
 * This class is only intended to be used by the [MapboxTripSession].
 *
 * Trip locations can come from the device, from an external location engine, or from a replay
 * simulator. This class is responsible for determining which LocationEngine is used.
 *
 * When a trip session is started with replay, use the [ReplayLocationEngine]. If the
 * trip session is not using replay, use the [NavigationOptions.locationEngine].
 */
internal class TripSessionLocationEngine constructor(
    private val navigationOptions: NavigationOptions,
    private val replayLocationEngineProvider: (MapboxReplayer) -> ReplayLocationEngine = {
        ReplayLocationEngine(it)
    }
) {

    val mapboxReplayer: MapboxReplayer by lazy { MapboxReplayer() }
    var isReplayEnabled = false
        private set

    private val replayLocationEngine: ReplayLocationEngine by lazy {
        val engine = replayLocationEngineProvider.invoke(mapboxReplayer)
        NavLocationService.addUserLiveTrackingClient(engine)
        engine
    }
//    private var activeLocationEngine: CancellableLocationEngine? = null
    private var onRawLocationUpdate: (Location) -> Unit = { }

    private val locationEngineCallback = object : LiveTrackingClientObserver {
        override fun onLiveTrackingStateChanged(state: LiveTrackingState, error: LocationError?) {
            // NO-OP
        }

        override fun onLocationUpdateReceived(locationUpdate: Expected<LocationError, MutableList<com.mapbox.common.location.Location>>) {
            locationUpdate.onValue(::onSuccess).onError(::onFailure)
        }

        private fun onSuccess(result: List<com.mapbox.common.location.Location>) {
            logD(LOG_CATEGORY) {
                "successful location engine callback $result"
            }
            result.lastOrNull()?.let {
                val location = it.toAndroidLocation()
                logIfLocationIsNotFreshEnough(location)
                onRawLocationUpdate(location)
            }
        }

        private fun onFailure(locationError: LocationError) {
            logD("location on failure exception=$locationError", LOG_CATEGORY)
        }
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates(isReplayEnabled: Boolean, onRawLocationUpdate: (Location) -> Unit) {
        logD(LOG_CATEGORY) {
            "starting location updates for ${if (isReplayEnabled) "replay " else ""}location engine"
        }
        stopLocationUpdates()
        this.onRawLocationUpdate = onRawLocationUpdate
//        activeLocationEngine = CancellableLocationEngine(
//            if (isReplayEnabled) {
//                replayLocationEngine
//            } else {
//                navigationOptions.locationEngine
//            }
//        )
        this.isReplayEnabled = isReplayEnabled
        val liveTrackingClientName: String? = if (isReplayEnabled) replayLocationEngine.name else navigationOptions.locationEngine?.name
        // TODO: Somehow take into account the navigationOptions.locationEngineRequest
        val activeLocationEngineResult = LocationServiceFactory.locationService().getLiveTrackingClient(liveTrackingClientName, null)
        activeLocationEngineResult.onValue { engine ->
            engine.registerObserver(locationEngineCallback)
        }
//        activeLocationEngine?.requestLocationUpdates(
//            navigationOptions.locationEngineRequest,
//            locationEngineCallback,
//            Looper.getMainLooper()
//        )
        // TODO: For real, we need get last location in client
//        activeLocationEngine?.getLastLocation(locationEngineCallback)
    }

    fun stopLocationUpdates() {
        val liveTrackingClientName = if (isReplayEnabled) replayLocationEngine.name else navigationOptions.locationEngine?.name
        // TODO: Somehow take into account the navigationOptions.locationEngineRequest
        val activeLocationEngineResult = LocationServiceFactory.locationService().getLiveTrackingClient(liveTrackingClientName, null)
        if (isReplayEnabled) {
            replayLocationEngine.cleanUpLastLocation()
        }
        isReplayEnabled = false
        onRawLocationUpdate = { }
        activeLocationEngineResult.onValue { engine ->
//            engine.cancelLastLocationTask(locationEngineCallback)
            engine.unregisterObserver(locationEngineCallback)
        }
    }

    private fun logIfLocationIsNotFreshEnough(location: Location) {
        val currentTime = SystemClock.elapsedRealtimeNanos()
        val locationAgeNanoSeconds = currentTime - location.elapsedRealtimeNanos
        val locationAgeMilliseconds = TimeUnit.MILLISECONDS.convert(
            locationAgeNanoSeconds,
            TimeUnit.NANOSECONDS
        )
        if (locationAgeMilliseconds > DELAYED_LOCATION_WARNING_THRESHOLD_MS) {
            logW("Got an obsolete location: age = $locationAgeMilliseconds ms", LOG_CATEGORY)
        }
    }

    private companion object {
        private const val DELAYED_LOCATION_WARNING_THRESHOLD_MS = 500 // 0.5s
        private const val LOG_CATEGORY = "TripSessionLocationEngine"
    }
}
