package com.mapbox.navigation.base.internal.factory

import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.trip.model.RouteStepProgress

/**
 * Internal factory to build [RouteStepProgress] objects
 */
object RouteStepProgressFactory {

    /**
     * Build a [RouteStepProgress] object
     *
     * @param stepIndex [Int] Index representing the current step the user is on
     * @param intersectionIndex [Int] Index representing the current intersection the user is on
     * @param step [LegStep] Returns the current step the user is traversing along
     * @param stepPoints [List][Point] that represent the current step geometry
     * @param distanceRemaining [Float] Total distance in meters from user to end of step
     * @param distanceTraveled [Float] Distance user has traveled along current step in unit meters
     * @param fractionTraveled [Float] The fraction traveled along the current step, this is a
     * float value between 0 and 1 and isn't guaranteed to reach 1 before the user reaches the
     * next step (if another step exist in route)
     * @param durationRemaining [Double] The duration remaining in seconds until the user reaches the end of the current step
     */
    fun buildRouteStepProgressObject(
        stepIndex: Int = 0,
        intersectionIndex: Int = 0,
        step: LegStep? = null,
        stepPoints: List<Point>? = null,
        distanceRemaining: Float = 0f,
        distanceTraveled: Float = 0f,
        fractionTraveled: Float = 0f,
        durationRemaining: Double = 0.0
    ): RouteStepProgress {
        return RouteStepProgress(
            stepIndex,
            intersectionIndex,
            step,
            stepPoints,
            distanceRemaining,
            distanceTraveled,
            fractionTraveled,
            durationRemaining
        )
    }
}
