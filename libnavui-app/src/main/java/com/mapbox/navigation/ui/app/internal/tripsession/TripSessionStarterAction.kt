package com.mapbox.navigation.ui.app.internal.tripsession

import com.mapbox.navigation.core.trip.MapboxTripStarter
import com.mapbox.navigation.ui.app.internal.Action

/**
 * Defines actions responsible to mutating the [MapboxTripStarter].
 */
sealed class TripSessionStarterAction : Action {
    /**
     * The action informs refreshes the internal state for location permissions.
     */
    object RefreshLocationPermissions : TripSessionStarterAction()

    /**
     * The action enables trip session based on real gps updates.
     */
    object EnableTripSession : TripSessionStarterAction()

    /**
     * The action enables replay trip session based on simulated gps updates.
     */
    object EnableReplayTripSession : TripSessionStarterAction()
}
