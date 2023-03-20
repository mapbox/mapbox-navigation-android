package com.mapbox.navigation.core.trip.session

import android.annotation.SuppressLint
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineResult

/**
 * LocationEngine does not have a method to cancel get last location task (CORESDK-1819),
 * as it has `removeLocationUpdates` for `requestLocationUpdates`.
 * This may lead to getLastLocation callback firing after `stopLocationUpdates` was invoked,
 * which is not expected behaviour in context of Mapbox trip sessions.
 * This class introduces an ability to cancel get last location task for a particular callback.
 * The issue is reproducible with `ReplayLocationTest#replay_session_locations_do_not_contain_locations_from_previous_session`.
 */
internal class CancellableLocationEngine(
    private val originalEngine: LocationEngine
) : LocationEngine by originalEngine {

    private val lastLocationCallbacks =
        mutableListOf<LocationEngineCallback<LocationEngineResult>>()

    @SuppressLint("MissingPermission")
    override fun getLastLocation(callback: LocationEngineCallback<LocationEngineResult>) {
        lastLocationCallbacks.add(callback)
        originalEngine.getLastLocation(object : LocationEngineCallback<LocationEngineResult> {

            override fun onSuccess(result: LocationEngineResult?) {
                if (lastLocationCallbacks.remove(callback)) {
                    callback.onSuccess(result)
                }
            }

            override fun onFailure(exception: Exception) {
                if (lastLocationCallbacks.remove(callback)) {
                    callback.onFailure(exception)
                }
            }
        })
    }

    /**
     * Cancel getLastLocation for a specified callback.
     * If original engine triggers callback for getLastLocation task after this method is invoked,
     * the update will not be proxied to the original callback.
     */
    fun cancelLastLocationTask(callback: LocationEngineCallback<LocationEngineResult>) {
        lastLocationCallbacks.remove(callback)
    }
}
