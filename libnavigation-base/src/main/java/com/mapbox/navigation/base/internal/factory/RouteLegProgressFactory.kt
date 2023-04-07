package com.mapbox.navigation.base.internal.factory

import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.route.LegWaypoint
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteStepProgress

/**
 * Internal factory to build [RouteLegProgress] objects
 */
@ExperimentalMapboxNavigationAPI
object RouteLegProgressFactory {

    /**
     * Build a [RouteLegProgress] object
     *
     * @param legIndex Index representing the current leg the user is on. If the directions route currently in use
     * contains more then two waypoints, the route is likely to have multiple legs representing the
     * distance between the two points.
     * @param routeLeg [RouteLeg] geometry
     * @param distanceTraveled Total distance traveled in meters along current leg
     * @param distanceRemaining The distance remaining in meters until the user reaches the end of the leg
     * @param durationRemaining The duration remaining in seconds until the user reaches the end of the current step
     * @param fractionTraveled The fraction traveled along the current leg, this is a float value between 0 and 1 and
     * isn't guaranteed to reach 1 before the user reaches the next waypoint
     * @param currentStepProgress [RouteStepProgress] object with information about the particular step the user
     * is currently on
     * @param upcomingStep Next/upcoming step immediately after the current step. If the user is on the last step
     * on the last leg, this will return null since a next step doesn't exist
     * @param geometryIndex Leg-wise index representing the geometry point that starts the segment
     * the user is currently on, effectively this represents the index of last visited geometry point in the leg.
     * @param legDestination the waypoint that marks the end of the current leg.
     */
    fun buildRouteLegProgressObject(
        legIndex: Int,
        routeLeg: RouteLeg?,
        distanceTraveled: Float,
        distanceRemaining: Float,
        durationRemaining: Double,
        fractionTraveled: Float,
        currentStepProgress: RouteStepProgress?,
        upcomingStep: LegStep?,
        geometryIndex: Int,
        legDestination: LegWaypoint?,
    ): RouteLegProgress {
        return RouteLegProgress(
            legIndex = legIndex,
            routeLeg = routeLeg,
            distanceTraveled = distanceTraveled,
            distanceRemaining = distanceRemaining,
            durationRemaining = durationRemaining,
            fractionTraveled = fractionTraveled,
            currentStepProgress = currentStepProgress,
            upcomingStep = upcomingStep,
            geometryIndex = geometryIndex,
            legDestination = legDestination,
        )
    }
}
