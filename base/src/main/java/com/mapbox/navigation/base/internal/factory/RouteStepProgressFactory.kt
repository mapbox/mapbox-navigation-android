package com.mapbox.navigation.base.internal.factory

import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.trip.model.RouteStepProgress

/**
 * Internal factory to build [RouteStepProgress] objects
 */
@ExperimentalMapboxNavigationAPI
object RouteStepProgressFactory {

    /**
     * Build a [RouteStepProgress] object
     *
     * @param stepIndex [Int] Index representing the current step the user is on
     * @param intersectionIndex [Int] Index representing the current intersection the user is on
     * @param instructionIndex [Int] Index of the current instruction in the list of available instructions for this step
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
        stepIndex: Int,
        intersectionIndex: Int,
        instructionIndex: Int?,
        step: LegStep?,
        stepPoints: List<Point>?,
        distanceRemaining: Float,
        distanceTraveled: Float,
        fractionTraveled: Float,
        durationRemaining: Double,
    ): RouteStepProgress {
        return RouteStepProgress(
            stepIndex = stepIndex,
            intersectionIndex = intersectionIndex,
            instructionIndex = instructionIndex,
            step = step,
            stepPoints = stepPoints,
            distanceRemaining = distanceRemaining,
            distanceTraveled = distanceTraveled,
            fractionTraveled = fractionTraveled,
            durationRemaining = durationRemaining,
        )
    }
}
