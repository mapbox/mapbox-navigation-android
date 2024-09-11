package com.mapbox.navigation.tripdata.maneuver.model

import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.formatter.DistanceFormatter

/**
 * A factory exposed to build [StepDistance] object.
 */
@ExperimentalMapboxNavigationAPI
object StepDistanceFactory {

    /**
     * Build [StepDistance] given appropriate arguments.
     */
    @JvmStatic
    fun buildStepDistance(
        distanceFormatter: DistanceFormatter,
        totalDistance: Double,
        distanceRemaining: Double?,
    ) = StepDistance(distanceFormatter, totalDistance, distanceRemaining)
}
