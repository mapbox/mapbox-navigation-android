package com.mapbox.navigation.core

/**
 * Callback invoked when [MapboxNavigation.resetTripSession] finishes.
 */
fun interface TripSessionResetCallback {
    /**
     * Invoked when resetting the trip finishes and the matching history is removed
     * which can be helpful to transition the user to new location in simulated environments.
     */
    fun onTripSessionReset()
}
