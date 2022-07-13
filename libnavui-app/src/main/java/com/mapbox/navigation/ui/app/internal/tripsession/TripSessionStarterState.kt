package com.mapbox.navigation.ui.app.internal.tripsession

/**
 * Defines the state for trip session
 * @param isLocationPermissionGranted informs if location permissions are already granted
 * @param isReplayEnabled is set to true if enabled; false otherwise
 */
data class TripSessionStarterState internal constructor(
    val isLocationPermissionGranted: Boolean = false,
    val isReplayEnabled: Boolean = false,
)
