package com.mapbox.navigation.core.telemetry

import android.location.Location

internal class EventLocations(
    private val preEventLocations: List<Location>,
    private val postEventLocations: MutableList<Location>,
    private val onBufferFull: (List<Location>, List<Location>) -> Unit
) {
    fun onBufferFull() {
        onBufferFull(preEventLocations, postEventLocations)
    }

    fun addPostEventLocation(location: Location) {
        postEventLocations.add(location)
    }

    fun postEventLocationsSize() = postEventLocations.size
}
