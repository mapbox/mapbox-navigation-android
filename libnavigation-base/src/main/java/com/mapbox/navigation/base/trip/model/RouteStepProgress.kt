package com.mapbox.navigation.base.trip.model

import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.geojson.Point

/**
 * This is a progress object specific to the current step the user is on.
 *
 * The latest route step progress object can be obtained through the [com.mapbox.navigation.core.trip.session.RouteProgressObserver].
 * Note that the route step progress object's immutable.
 */
class RouteStepProgress private constructor(
    private val stepIndex: Int = 0,
    private val step: LegStep? = null,
    private val stepPoints: List<Point>? = null,
    private val distanceRemaining: Float = 0f,
    private val distanceTraveled: Float = 0f,
    private val fractionTraveled: Float = 0f,
    private val durationRemaining: Double = 0.0,
    private val guidanceViewURL: String? = null,
    private val builder: Builder
) {

    /**
     * Index representing the current step the user is on.
     *
     * @return an integer representing the current step the user is on
     */
    fun stepIndex(): Int = stepIndex

    /**
     * Returns the current step the user is traversing along.
     *
     * @return a [LegStep] representing the step the user is currently on
     */
    fun step(): LegStep? = step

    /**
     * Provides a list of points that represent the current step
     * step geometry.
     *
     * @return list of points representing the current step
     */
    fun stepPoints() = stepPoints

    /**
     * Total distance in meters from user to end of step.
     *
     * @return float value representing the distance the user has remaining till they reach the end
     * of the current step. Uses unit meters.
     */
    fun distanceRemaining(): Float = distanceRemaining

    /**
     * Returns distance user has traveled along current step in unit meters.
     *
     * @return double value representing the distance the user has traveled so far along the current
     * step. Uses unit meters.
     */
    fun distanceTraveled(): Float = distanceTraveled

    /**
     * Get the fraction traveled along the current step, this is a float value between 0 and 1 and
     * isn't guaranteed to reach 1 before the user reaches the next step (if another step exist in route).
     *
     * @return a float value between 0 and 1 representing the fraction the user has traveled along
     * the current step.
     */
    fun fractionTraveled(): Float = fractionTraveled

    /**
     * Provides the duration remaining in seconds till the user reaches the end of the current step.
     *
     * @return duration remaining till end of step, in unit seconds.
     */
    fun durationRemaining(): Double = durationRemaining

    /**
     * Provides guidance image URL
     *
     * @return `long` value representing the duration remaining till end of step, in unit seconds.
     */
    fun guidanceViewURL(): String? = guidanceViewURL

    /**
     * Get a builder to customize a subset of current options.
     */
    fun toBuilder() = builder

    /**
     * Builder of [RouteStepProgress]
     *
     * @param stepIndex Index representing the current step the user is on
     * @param step Returns the current step the user is traversing along
     * @param stepPoints A list of points that represent the current step geometry
     * @param distanceRemaining Total distance in meters from user to end of step
     * @param distanceTraveled Distance user has traveled along current step in unit meters
     * @param fractionTraveled The fraction traveled along the current step, this is a float value between 0 and 1 and
     * isn't guaranteed to reach 1 before the user reaches the next step (if another step exist in route)
     * @param durationRemaining The duration remaining in seconds until the user reaches the end of the current step
     * @param guidanceViewURL Guidance image URL
     */
    data class Builder(
        private var stepIndex: Int = 0,
        private var step: LegStep? = null,
        private var stepPoints: List<Point>? = null,
        private var distanceRemaining: Float = 0f,
        private var distanceTraveled: Float = 0f,
        private var fractionTraveled: Float = 0f,
        private var durationRemaining: Double = 0.0,
        private var guidanceViewURL: String? = null
    ) {

        /**
         * Index representing the current step the user is on
         *
         * @return Builder
         */
        fun stepIndex(stepIndex: Int) =
            apply { this.stepIndex = stepIndex }

        /**
         * Returns the current step the user is traversing along
         *
         * @return Builder
         */
        fun step(step: LegStep) = apply { this.step = step }

        /**
         * A list of points that represent the current step geometry
         *
         * @return Builder
         */
        fun stepPoints(stepPoints: List<Point>) =
            apply { this.stepPoints = stepPoints }

        /**
         * Total distance in meters from user to end of step
         *
         * @return Builder
         */
        fun distanceRemaining(distanceRemaining: Float) =
            apply { this.distanceRemaining = distanceRemaining }

        /**
         * Returns distance user has traveled along current step in unit meters
         *
         * @return Builder
         */
        fun distanceTraveled(distanceTraveled: Float) =
            apply { this.distanceTraveled = distanceTraveled }

        /**
         * The fraction traveled along the current step. This is a float value between 0 and 1 and
         * isn't guaranteed to reach 1 before the user reaches the next step (if another step exist in route)
         *
         * @return Builder
         */
        fun fractionTraveled(fractionTraveled: Float) =
            apply { this.fractionTraveled = fractionTraveled }

        /**
         * The duration remaining in seconds until the user reaches the end of the current step
         *
         * @return Builder
         */
        fun durationRemaining(durationRemaining: Double) =
            apply { this.durationRemaining = durationRemaining }

        /**
         * Guidance image URL
         *
         * @return Builder
         */
        fun guidanceViewURL(guidanceURL: String?) =
            apply { this.guidanceViewURL = guidanceURL }

        /**
         * Build new instance of [RouteStepProgress]
         *
         * @return RouteStepProgress
         */
        fun build(): RouteStepProgress {
            return RouteStepProgress(
                stepIndex,
                step,
                stepPoints,
                distanceRemaining,
                distanceTraveled,
                fractionTraveled,
                durationRemaining,
                guidanceViewURL,
                this
            )
        }
    }
}
