package com.mapbox.navigation.core.infra

import android.app.PendingIntent
import android.location.Location
import android.os.Looper
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.navigation.testing.factories.createLocation

class TestLocationEngine private constructor() : LocationEngine {

    companion object {

        fun create() = TestLocationEngine()
    }

    private val locationUpdateCallbacks =
        mutableListOf<LocationEngineCallback<LocationEngineResult>>()
    private var currentLocation = LocationEngineResult.create(createLocation())

    override fun getLastLocation(p0: LocationEngineCallback<LocationEngineResult>) {
        p0.onSuccess(currentLocation)
    }

    override fun requestLocationUpdates(
        p0: LocationEngineRequest,
        p1: LocationEngineCallback<LocationEngineResult>,
        p2: Looper?
    ) {
        locationUpdateCallbacks.add(p1)
    }

    override fun removeLocationUpdates(p0: LocationEngineCallback<LocationEngineResult>) {
        locationUpdateCallbacks.remove(p0)
    }

    override fun requestLocationUpdates(p0: LocationEngineRequest, p1: PendingIntent?) {
        TODO(
            "requestLocationUpdates for PendingIntent isn't supported yet." +
                "Implement if you need it"
        )
    }

    override fun removeLocationUpdates(p0: PendingIntent?) {
        TODO(
            "removeLocationUpdates for PendingIntent isn't supported yet." +
                "Implement if you need it"
        )
    }

    fun updateLocation(location: Location) {
        currentLocation = LocationEngineResult.create(location)
        locationUpdateCallbacks.forEach { it.onSuccess(currentLocation) }
    }
}
