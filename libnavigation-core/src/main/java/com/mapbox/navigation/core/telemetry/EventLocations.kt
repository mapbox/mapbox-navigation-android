package com.mapbox.navigation.core.telemetry

import com.mapbox.common.location.Location

internal class EventLocations(
    private val preEventLocations: List<Location>,
    private val postEventLocations: MutableList<Location>,
    val locationsCollectorListener: LocationsCollector.LocationsCollectorListener,
) {
    fun onBufferFull() {
        locationsCollectorListener.onBufferFull(preEventLocations, postEventLocations)
    }

    fun addPostEventLocation(location: Location) {
        postEventLocations.add(location)
    }

    fun postEventLocationsSize() = postEventLocations.size
}
