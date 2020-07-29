package com.mapbox.navigation.base.trip.model

import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.api.directions.v5.models.RouteLeg

/**
 * This is a progress object specific to the current leg the user is on. If there is only one leg
 * in the directions route, much of this information will be identical to the parent
 * [RouteProgress].
 *
 * The latest route leg progress object can be obtained through the [com.mapbox.navigation.core.trip.session.RouteProgressObserver].
 * Note that the route leg progress object's immutable.
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
 */
class RouteLegProgress private constructor(
    val legIndex: Int,
    val routeLeg: RouteLeg?,
    val distanceTraveled: Float,
    val distanceRemaining: Float,
    val durationRemaining: Double,
    val fractionTraveled: Float,
    val currentStepProgress: RouteStepProgress?,
    val upcomingStep: LegStep?
) {

    /**
     * @return builder matching the one used to create this instance
     */
    fun toBuilder() = Builder()
        .legIndex(legIndex)
        .routeLeg(routeLeg)
        .distanceTraveled(distanceTraveled)
        .distanceRemaining(distanceRemaining)
        .durationRemaining(durationRemaining)
        .fractionTraveled(fractionTraveled)
        .currentStepProgress(currentStepProgress)
        .upcomingStep(upcomingStep)

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

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = legIndex
        result = 31 * result + (routeLeg?.hashCode() ?: 0)
        result = 31 * result + distanceTraveled.hashCode()
        result = 31 * result + distanceRemaining.hashCode()
        result = 31 * result + durationRemaining.hashCode()
        result = 31 * result + fractionTraveled.hashCode()
        result = 31 * result + (currentStepProgress?.hashCode() ?: 0)
        result = 31 * result + (upcomingStep?.hashCode() ?: 0)
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "RouteLegProgress(legIndex=$legIndex, routeLeg=$routeLeg, distanceTraveled=$distanceTraveled, distanceRemaining=$distanceRemaining, durationRemaining=$durationRemaining, fractionTraveled=$fractionTraveled, currentStepProgress=$currentStepProgress, upcomingStep=$upcomingStep)"
    }

    /**
     * Builder of [RouteLegProgress].
     */
    class Builder {
        private var legIndex: Int = 0
        private var routeLeg: RouteLeg? = null
        private var distanceTraveled: Float = 0f
        private var distanceRemaining: Float = 0f
        private var durationRemaining: Double = 0.0
        private var fractionTraveled: Float = 0f
        private var currentStepProgress: RouteStepProgress? = null
        private var upcomingStep: LegStep? = null

        /**
         * Index representing the current leg the user is on. If the directions route currently in use
         * contains more then two waypoints, the route is likely to have multiple legs representing the
         * distance between the two points.
         *
         * @return Builder
         */
        fun legIndex(legIndex: Int) = apply { this.legIndex = legIndex }

        /**
         * [RouteLeg] geometry
         *
         * @return Builder
         */
        fun routeLeg(routeLeg: RouteLeg?) = apply { this.routeLeg = routeLeg }

        /**
         * Total distance traveled in meters along current leg
         *
         * @return Builder
         */
        fun distanceTraveled(distanceTraveled: Float) =
            apply { this.distanceTraveled = distanceTraveled }

        /**
         * The distance remaining in meters until the user reaches the end of the leg
         *
         * @return Builder
         */
        fun distanceRemaining(distanceRemaining: Float) =
            apply { this.distanceRemaining = distanceRemaining }

        /**
         * The duration remaining in seconds until the user reaches the end of the current step
         *
         * @return Builder
         */
        fun durationRemaining(durationRemaining: Double) =
            apply { this.durationRemaining = durationRemaining }

        /**
         * The fraction traveled along the current leg, this is a float value between 0 and 1 and
         * isn't guaranteed to reach 1 before the user reaches the next waypoint
         *
         * @return Builder
         */
        fun fractionTraveled(fractionTraveled: Float) =
            apply { this.fractionTraveled = fractionTraveled }

        /**
         * [RouteStepProgress] object with information about the particular step the user
         * is currently on
         *
         * @return Builder
         */
        fun currentStepProgress(currentStepProgress: RouteStepProgress?) =
            apply { this.currentStepProgress = currentStepProgress }

        /**
         * Next/upcoming step immediately after the current step. If the user is on the last step
         * on the last leg, this will return null since a next step doesn't exist
         *
         * @return Builder
         */
        fun upcomingStep(upcomingStep: LegStep?) =
                apply { this.upcomingStep = upcomingStep }

        /**
         * Build new instance of [RouteLegProgress]
         *
         * @return RouteLegProgress
         */
        fun build(): RouteLegProgress {
            return RouteLegProgress(
                legIndex,
                routeLeg,
                distanceTraveled,
                distanceRemaining,
                durationRemaining,
                fractionTraveled,
                currentStepProgress,
                upcomingStep
            )
        }
    }
}
