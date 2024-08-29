package com.mapbox.navigation.core.telemetry

import com.mapbox.common.location.Location
import com.mapbox.navigation.core.trip.session.LocationObserver

internal interface LocationsCollector : LocationObserver {
    val lastLocation: Location?

    fun flushBuffers()
    fun flushBufferFor(locationsCollectorListener: LocationsCollectorListener)
    fun collectLocations(locationsCollectorListener: LocationsCollectorListener)

    fun interface LocationsCollectorListener {
        fun onBufferFull(preEventLocations: List<Location>, postEventLocations: List<Location>)
    }
}
