package com.mapbox.navigation.base.trip.model

import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.navigation.base.route.LegWaypoint

/**
 * This is a progress object specific to the current leg the user is on. If there is only one leg
 * in the directions route, much of this information will be identical to the parent
 * [RouteProgress].
 *
 * The latest route leg progress object can be obtained through the [RouteProgressObserver].
 * Note that the route leg progress object's immutable.
 *
 * @param legIndex Index representing the current leg the user is on. If the directions route currently in use
 * contains more then two waypoints, the route is likely to have multiple legs representing the
 * distance between the two points.
 * @param routeLeg [RouteLeg] geometry
 * @param distanceTraveled Total distance traveled in meters along current leg
 * @param distanceRemaining The distance remaining in meters until the user reaches the end of the leg
 * @param durationRemaining The duration remaining in seconds until the user reaches the end of the leg
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
class RouteLegProgress internal constructor(
    val legIndex: Int,
    val routeLeg: RouteLeg?,
    val distanceTraveled: Float,
    val distanceRemaining: Float,
    val durationRemaining: Double,
    val fractionTraveled: Float,
    val currentStepProgress: RouteStepProgress?,
    val upcomingStep: LegStep?,
    val geometryIndex: Int,
    val legDestination: LegWaypoint?,
) {

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RouteLegProgress

        if (legIndex != other.legIndex) return false
        if (routeLeg != other.routeLeg) return false
        if (distanceTraveled != other.distanceTraveled) return false
        if (distanceRemaining != other.distanceRemaining) return false
        if (durationRemaining != other.durationRemaining) return false
        if (fractionTraveled != other.fractionTraveled) return false
        if (currentStepProgress != other.currentStepProgress) return false
        if (upcomingStep != other.upcomingStep) return false
        if (geometryIndex != other.geometryIndex) return false
        if (legDestination != other.legDestination) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = legIndex
        result = 31 * result + routeLeg.hashCode()
        result = 31 * result + distanceTraveled.hashCode()
        result = 31 * result + distanceRemaining.hashCode()
        result = 31 * result + durationRemaining.hashCode()
        result = 31 * result + fractionTraveled.hashCode()
        result = 31 * result + currentStepProgress.hashCode()
        result = 31 * result + upcomingStep.hashCode()
        result = 31 * result + geometryIndex.hashCode()
        result = 31 * result + legDestination.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "RouteLegProgress(" +
            "legIndex=$legIndex, " +
            "routeLeg=$routeLeg, " +
            "distanceTraveled=$distanceTraveled, " +
            "distanceRemaining=$distanceRemaining, " +
            "durationRemaining=$durationRemaining, " +
            "fractionTraveled=$fractionTraveled, " +
            "currentStepProgress=$currentStepProgress, " +
            "upcomingStep=$upcomingStep, " +
            "geometryIndex=$geometryIndex, " +
            "legDestination=$legDestination" +
            ")"
    }
}
