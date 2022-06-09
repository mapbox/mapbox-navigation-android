package com.mapbox.navigation.ui.app.internal.tripsession

import com.mapbox.navigation.ui.app.internal.Action

/**
 * Defines actions responsible to mutate the [TripSessionStarterState].
 */
sealed class TripSessionStarterAction : Action {
    /**
     * The action informs whether the location permissions have been granted.
     * @param granted is set to true if location permissions were granted; false otherwise
     */
    data class OnLocationPermission(val granted: Boolean) : TripSessionStarterAction()

    /**
     * The action enables trip session based on real gps updates.
     */
    object EnableTripSession : TripSessionStarterAction()

    /**
     * The action enables replay trip session based on simulated gps updates.
     */
    object EnableReplayTripSession : TripSessionStarterAction()
}
