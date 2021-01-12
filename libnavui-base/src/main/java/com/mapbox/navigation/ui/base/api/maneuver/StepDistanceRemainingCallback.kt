package com.mapbox.navigation.ui.base.api.maneuver

import com.mapbox.navigation.ui.base.model.maneuver.ManeuverState

/**
 * Interface definition for a callback to be invoked when a distance remaining to finish a step
 * is processed.
 */
interface StepDistanceRemainingCallback {

    /**
     * Invoked when distance remaining to finish a step is ready
     * @param distanceRemaining DistanceRemainingToFinishStep represents distance remaining.
     */
    fun onStepDistanceRemaining(distanceRemaining: ManeuverState.DistanceRemainingToFinishStep)
}
