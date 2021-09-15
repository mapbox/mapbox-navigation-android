package com.mapbox.navigation.core.trip.session

import android.annotation.SuppressLint
import android.location.Location
import android.os.Looper
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.base.common.logger.model.Message
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.utils.internal.LoggerProvider

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
    val navigationOptions: NavigationOptions
) {
    val mapboxReplayer: MapboxReplayer by lazy { MapboxReplayer() }

    private val replayLocationEngine: ReplayLocationEngine by lazy {
        ReplayLocationEngine(mapboxReplayer)
    }
    private var locationEngine: LocationEngine = navigationOptions.locationEngine
    private var onRawLocationUpdate: (Location) -> Unit = { }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates(isReplayEnabled: Boolean, onRawLocationUpdate: (Location) -> Unit) {
        this.onRawLocationUpdate = onRawLocationUpdate
        val locationEngine = if (isReplayEnabled) {
            replayLocationEngine
        } else {
            navigationOptions.locationEngine
        }
        locationEngine.requestLocationUpdates(
            navigationOptions.locationEngineRequest,
            locationEngineCallback,
            Looper.getMainLooper()
        )
        locationEngine.getLastLocation(locationEngineCallback)
    }

    fun stopLocationUpdates() {
        onRawLocationUpdate = { }
        locationEngine.removeLocationUpdates(locationEngineCallback)
    }

    private var locationEngineCallback = object : LocationEngineCallback<LocationEngineResult> {
        override fun onSuccess(result: LocationEngineResult?) {
            result?.locations?.lastOrNull()?.let {
                onRawLocationUpdate(it)
            }
        }

        override fun onFailure(exception: Exception) {
            LoggerProvider.logger.d(
                msg = Message("location on failure"),
                tr = exception
            )
        }
    }
}
