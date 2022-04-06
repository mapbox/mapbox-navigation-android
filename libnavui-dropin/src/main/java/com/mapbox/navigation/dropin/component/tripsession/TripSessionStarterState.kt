package com.mapbox.navigation.dropin.component.tripsession

data class TripSessionStarterState internal constructor(
    val isLocationPermissionGranted: Boolean = false,
    // TODO this is true for development. Road testing should be set to false.
    val isReplayEnabled: Boolean = true,
)
