package com.mapbox.navigation.ui.maneuver.api

import com.mapbox.navigation.ui.base.model.Expected
import com.mapbox.navigation.ui.maneuver.model.StepDistance
import com.mapbox.navigation.ui.maneuver.model.StepDistanceError

/**
 * Interface definition for a callback to be invoked when a distance remaining data is processed.
 */
interface StepDistanceRemainingCallback {

    /**
     * Invoked when distance remaining to finish a step is ready
     * @param distanceRemaining DistanceRemainingToFinishStep represents distance remaining.
     */
    fun onStepDistanceRemaining(distanceRemaining: Expected<StepDistance, StepDistanceError>)
}
