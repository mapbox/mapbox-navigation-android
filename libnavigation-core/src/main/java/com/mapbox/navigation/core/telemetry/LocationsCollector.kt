package com.mapbox.navigation.core.telemetry

import android.location.Location
import com.mapbox.navigation.core.trip.session.LocationObserver

internal interface LocationsCollector : LocationObserver {
    val lastLocation: Location?

    fun flushBuffers()
    fun collectLocations(onBufferFull: (List<Location>, List<Location>) -> Unit)
}
