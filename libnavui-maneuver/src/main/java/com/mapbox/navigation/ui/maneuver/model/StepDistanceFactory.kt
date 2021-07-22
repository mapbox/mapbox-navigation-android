package com.mapbox.navigation.ui.maneuver.model

import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.formatter.DistanceFormatter

/**
 * A factory exposed to build a [StepDistance] object.
 */
@ExperimentalMapboxNavigationAPI
object StepDistanceFactory {

    /**
     * Build [StepDistance] given appropriate arguments.
     */
    @JvmStatic
    fun buildStepDistance(
        formatter: DistanceFormatter,
        totalDistance: Double,
        distanceRemaining: Double?
    ) = StepDistance(formatter, totalDistance, distanceRemaining)
}
