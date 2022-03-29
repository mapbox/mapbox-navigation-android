package com.mapbox.navigation.dropin.component.tripsession

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.dropin.model.Action

/**
 * Defines actions responsible to mutate the [TripSessionStarterState].
 */
@ExperimentalPreviewMapboxNavigationAPI
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
