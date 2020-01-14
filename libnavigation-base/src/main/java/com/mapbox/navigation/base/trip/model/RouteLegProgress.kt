package com.mapbox.navigation.base.trip.model

import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.api.directions.v5.models.RouteLeg

class RouteLegProgress private constructor(
    private val legIndex: Int = 0,
    private val routeLeg: RouteLeg? = null,
    private val distanceTraveled: Float = 0f,
    private val distanceRemaining: Float = 0f,
    private val durationRemaining: Long = 0L,
    private val fractionTraveled: Float = 0f,
    private val currentStepProgress: RouteStepProgress? = null,
    private val upcomingStep: LegStep? = null,
    private val builder: Builder
) {

    /**
     * Index representing the current leg the user is on. If the directions route currently in use
     * contains more then two waypoints, the route is likely to have multiple legs representing the
     * distance between the two points.
     *
     * @return an integer representing the current leg the user is on
     */
    fun legIndex(): Int = legIndex

    /**
     * This [RouteLeg] geometry.
     *
     * @return route leg geometry
     */
    fun routeLeg(): RouteLeg? = routeLeg

    /**
     * Total distance traveled in meters along current leg.
     *
     * @return a double value representing the total distance the user has traveled along the current
     * leg, using unit meters.
     */
    fun distanceTraveled(): Float = distanceTraveled

    /**
     * Provides the duration remaining in seconds till the user reaches the end of the route.
     *
     * @return long value representing the duration remaining till end of route, in unit seconds
     */
    fun distanceRemaining(): Float = distanceRemaining

    /**
     * Provides the duration remaining in seconds till the user reaches the end of the current step.
     *
     * @return long value representing the duration remaining till end of step, in unit seconds.
     */
    fun durationRemaining(): Long = durationRemaining

    /**
     * Get the fraction traveled along the current leg, this is a float value between 0 and 1 and
     * isn't guaranteed to reach 1 before the user reaches the next waypoint.
     *
     * @return a float value between 0 and 1 representing the fraction the user has traveled along the
     * current leg
     */
    fun fractionTraveled(): Float = fractionTraveled

    /**
     * Gives a [RouteStepProgress] object with information about the particular step the user
     * is currently on.
     *
     * @return a [RouteStepProgress] object
     */
    fun currentStepProgress(): RouteStepProgress? = currentStepProgress

    /**
     * Get the next/upcoming step immediately after the current step. If the user is on the last step
     * on the last leg, this will return null since a next step doesn't exist.
     *
     * @return a [LegStep] representing the next step the user will be on.
     */
    fun upcomingStep(): LegStep? = upcomingStep

    fun toBuilder() = builder

    data class Builder(
        private var legIndex: Int = 0,
        private var routeLeg: RouteLeg? = null,
        private var distanceTraveled: Float = 0f,
        private var distanceRemaining: Float = 0f,
        private var durationRemaining: Long = 0L,
        private var fractionTraveled: Float = 0f,
        private var currentStepProgress: RouteStepProgress? = null,
        private var upcomingStep: LegStep? = null
    ) {

        fun legIndex(legIndex: Int) = apply { this.legIndex = legIndex }

        fun routeLeg(routeLeg: RouteLeg) = apply { this.routeLeg = routeLeg }

        fun distanceTraveled(distanceTraveled: Float) =
            apply { this.distanceTraveled = distanceTraveled }

        fun distanceRemaining(distanceRemaining: Float) =
            apply { this.distanceRemaining = distanceRemaining }

        fun durationRemaining(durationRemaining: Long) =
            apply { this.durationRemaining = durationRemaining }

        fun fractionTraveled(fractionTraveled: Float) =
            apply { this.fractionTraveled = fractionTraveled }

        fun currentStepProgress(currentStepProgress: RouteStepProgress) =
            apply { this.currentStepProgress = currentStepProgress }

        fun upcomingStep(upcomingStep: LegStep) =
            apply { this.upcomingStep = upcomingStep }

        fun build(): RouteLegProgress {
            return RouteLegProgress(
                legIndex,
                routeLeg,
                distanceTraveled,
                distanceRemaining,
                durationRemaining,
                fractionTraveled,
                currentStepProgress,
                upcomingStep,
                this
            )
        }
    }
}
