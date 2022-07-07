package com.mapbox.navigation.ui.app.internal.tripsession

/**
 * Defines the state for trip session
 * @param isLocationPermissionGranted location permissions state.
 * Value can be `true`, `false` or `null` if permissions state hasn't been determined yet.
 * @param isReplayEnabled is set to true if enabled; false otherwise
 */
data class TripSessionStarterState internal constructor(
    val isLocationPermissionGranted: Boolean? = null,
    val isReplayEnabled: Boolean = false,
)
