package com.mapbox.navigation.core.trip.session

import android.annotation.SuppressLint
import android.location.Location
import android.os.Looper
import android.os.SystemClock
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.navigation.base.options.NavigationOptions
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
        replayLocationEngineProvider.invoke(mapboxReplayer)
    }
    private var activeLocationEngine: LocationEngine? = null
    private var onRawLocationUpdate: (Location) -> Unit = { }

    private var locationEngineCallback = createLocationEngineCallback()

    @SuppressLint("MissingPermission")
    fun startLocationUpdates(isReplayEnabled: Boolean, onRawLocationUpdate: (Location) -> Unit) {
        logD(LOG_CATEGORY) {
            "starting location updates for ${if (isReplayEnabled) "replay " else ""}location engine"
        }
        stopLocationUpdates()
        this.onRawLocationUpdate = onRawLocationUpdate
        activeLocationEngine = if (isReplayEnabled) {
            replayLocationEngine
        } else {
            navigationOptions.locationEngine
        }
        this.isReplayEnabled = isReplayEnabled
        activeLocationEngine?.requestLocationUpdates(
            navigationOptions.locationEngineRequest,
            locationEngineCallback,
            Looper.getMainLooper()
        )
        activeLocationEngine?.getLastLocation(locationEngineCallback)
    }

    fun stopLocationUpdates() {
        if (isReplayEnabled) {
            replayLocationEngine.cleanUpLastLocation()
        }
        isReplayEnabled = false
        onRawLocationUpdate = { }
        activeLocationEngine?.removeLocationUpdates(locationEngineCallback)
        locationEngineCallback = createLocationEngineCallback()
        activeLocationEngine = null
    }

    private fun createLocationEngineCallback(): LocationEngineCallback<LocationEngineResult> {
        return object : LocationEngineCallback<LocationEngineResult> {
            override fun onSuccess(result: LocationEngineResult?) {
                // ignore last location updates from previous session
                // (possible with last location callbacks: they can't be removed)
                // reproducible with ReplayLocationTest#last_location_is_cleared_when_session_is_stopped
                if (locationEngineCallback != this) return
                logD(LOG_CATEGORY) {
                    "successful location engine callback $result"
                }
                result?.locations?.lastOrNull()?.let {
                    logIfLocationIsNotFreshEnough(it)
                    onRawLocationUpdate(it)
                }
            }

            override fun onFailure(exception: Exception) {
                logD("location on failure exception=$exception", LOG_CATEGORY)
            }
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
