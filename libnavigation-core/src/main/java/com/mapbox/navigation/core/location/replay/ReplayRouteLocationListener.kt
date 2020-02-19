package com.mapbox.navigation.core.location.replay

import android.location.Location
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.navigation.core.location.ReplayRouteLocationEngine

internal class ReplayRouteLocationListener(
    private val engine: ReplayRouteLocationEngine,
    private val callback: LocationEngineCallback<LocationEngineResult>
) : ReplayLocationListener {

    override fun onLocationReplay(location: Location) {
        engine.updateLastLocation(location)
        engine.removeLastMockedLocation()
        val result = LocationEngineResult.create(location)
        callback.onSuccess(result)
    }
}
