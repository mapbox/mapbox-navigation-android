package com.mapbox.navigation.base.trip.model

import com.mapbox.api.directions.v5.models.DirectionsResponse

/**
 * An Electronic Horizon is a path (or paths) within the road graph which is used to surface
 * metadata about the underlying links of the graph for a certain distance in front of the vehicle.
 * The Electronic Horizon module correlates the vehicles location to the road graph and broadcasts
 * updates to the Electronic Horizon as the vehicles position and trajectory change.
 *
 * Electronic Horizon is still **experimental**, which means that the design of the
 * APIs has open issues which may (or may not) lead to their changes in the future.
 * Roughly speaking, there is a chance that those declarations will be deprecated in the near
 * future or the semantics of their behavior may change in some way that may break some code.
 *
 * Note that the Electronic Horizon object's immutable.
 */
class ElectronicHorizon private constructor(
    private val horizon: DirectionsResponse? = null,
    private val routeIndex: Int = 0,
    private val legIndex: Int = 0,
    private val legDistanceRemaining: Float = 0f,
    private val legDurationRemaining: Double = 0.0,
    private val stepIndex: Int = 0,
    private val stepDistanceRemaining: Float = 0f,
    private val stepDurationRemaining: Double = 0.0,
    private val shapeIndex: Int = 0,
    private val intersectionIndex: Int = 0,
    private val builder: Builder
) {

    /**
     * [DirectionsResponse] including MPP. MPP is a route that consists of 1 leg.
     *
     * For now, no alternatives are returned.
     *
     * @return a [DirectionsResponse] object
     */
    fun horizon(): DirectionsResponse? = horizon

    /**
     * Index representing the current route the user is on.
     *
     * For now, always 0 since only MPP is returned. MPP is a route that consists of 1 leg.
     *
     * @return an integer representing the current route the user is on
     */
    fun routeIndex(): Int = routeIndex

    /**
     * Index representing the current leg the user is on.
     *
     * For now, always 0 since only MPP is returned. MPP is a route that consists of 1 leg.
     *
     * @return an integer representing the current leg the user is on
     */
    fun legIndex(): Int = legIndex

    /**
     * Provides the distance remaining in meters until the user reaches the end of the leg.
     * Distance until the end of the Electronic Horizon itself.
     *
     * @return distance remaining till end of leg, in unit meters.
     */
    fun legDistanceRemaining(): Float = legDistanceRemaining

    /**
     * Provides the duration remaining in seconds until the user reaches the end of the current leg.
     * Duration until the end of the Electronic Horizon itself.
     *
     * @return duration remaining till end of leg, in unit seconds.
     */
    fun legDurationRemaining(): Double = legDurationRemaining

    /**
     * Index representing the current step the user is on.
     *
     * @return an integer representing the current step the user is on
     */
    fun stepIndex(): Int = stepIndex

    /**
     * Provides the distance remaining in meters until the user reaches the end of the step.
     *
     * @return distance remaining till end of step, in unit meters.
     */
    fun stepDistanceRemaining(): Float = stepDistanceRemaining

    /**
     * Provides the duration remaining in seconds until the user reaches the end of the current step.
     *
     * @return duration remaining till end of step, in unit seconds.
     */
    fun stepDurationRemaining(): Double = stepDurationRemaining

    /**
     * Index representing the segment of the MPP geometry the user is on.
     *
     * @return an integer representing the current segment of the MPP geometry the user is on
     */
    fun shapeIndex(): Int = shapeIndex

    /**
     * Index representing the upcoming intersection.
     *
     * @return an integer representing the upcoming intersection
     */
    fun intersectionIndex(): Int = intersectionIndex

    /**
     * Get a builder to customize a subset of current options.
     */
    fun toBuilder() = builder

    /**
     * Builder of [ElectronicHorizon]
     *
     * @param horizon [DirectionsResponse] including MPP
     * @param routeIndex index representing the current route the user is on
     * @param legIndex index representing the current leg the user is on
     * @param legDistanceRemaining distance remaining till end of leg, in unit meters
     * @param legDurationRemaining duration remaining till end of leg, in unit seconds
     * @param stepIndex index representing the current step the user is on
     * @param stepDistanceRemaining distance remaining till end of step, in unit meters
     * @param stepDurationRemaining duration remaining till end of step, in unit seconds
     * @param shapeIndex index representing the segment of the MPP geometry the user is on
     * @param intersectionIndex index representing the upcoming intersection
     */
    data class Builder(
        private var horizon: DirectionsResponse? = null,
        private var routeIndex: Int = 0,
        private var legIndex: Int = 0,
        private var legDistanceRemaining: Float = 0f,
        private var legDurationRemaining: Double = 0.0,
        private var stepIndex: Int = 0,
        private var stepDistanceRemaining: Float = 0f,
        private var stepDurationRemaining: Double = 0.0,
        private var shapeIndex: Int = 0,
        private var intersectionIndex: Int = 0
    ) {

        /**
         * [DirectionsResponse] including MPP.
         *
         * @return Builder
         */
        fun horizon(horizon: DirectionsResponse) = apply { this.horizon = horizon }

        /**
         * Index representing the current route the user is on.
         *
         * @return Builder
         */
        fun routeIndex(routeIndex: Int) = apply { this.routeIndex = routeIndex }

        /**
         * Index representing the current leg the user is on.
         *
         * @return Builder
         */
        fun legIndex(legIndex: Int) = apply { this.legIndex = legIndex }

        /**
         * Distance remaining till end of leg, in unit meters.
         *
         * @return Builder
         */
        fun legDistanceRemaining(legDistanceRemaining: Float) =
            apply { this.legDistanceRemaining = legDistanceRemaining }

        /**
         * Duration remaining till end of leg, in unit seconds.
         *
         * @return Builder
         */
        fun legDurationRemaining(legDurationRemaining: Double) =
            apply { this.legDurationRemaining = legDurationRemaining }

        /**
         * Index representing the current step the user is on.
         *
         * @return Builder
         */
        fun stepIndex(stepIndex: Int) = apply { this.stepIndex = stepIndex }

        /**
         * Distance remaining till end of step, in unit meters.
         *
         * @return Builder
         */
        fun stepDistanceRemaining(stepDistanceRemaining: Float) =
            apply { this.stepDistanceRemaining = stepDistanceRemaining }

        /**
         * Duration remaining till end of step, in unit seconds.
         *
         * @return Builder
         */
        fun stepDurationRemaining(stepDurationRemaining: Double) =
            apply { this.stepDurationRemaining = stepDurationRemaining }

        /**
         * Index representing the segment of the MPP geometry the user is on.
         *
         * @return Builder
         */
        fun shapeIndex(shapeIndex: Int) = apply { this.shapeIndex = shapeIndex }

        /**
         * Index representing the upcoming intersection.
         *
         * @return Builder
         */
        fun intersectionIndex(intersectionIndex: Int) = apply { this.intersectionIndex = intersectionIndex }

        /**
         * Build new instance of [ElectronicHorizon]
         *
         * @return RouteLegProgress
         */
        fun build(): ElectronicHorizon {
            return ElectronicHorizon(
                horizon,
                routeIndex,
                legIndex,
                legDistanceRemaining,
                legDurationRemaining,
                stepIndex,
                stepDistanceRemaining,
                stepDurationRemaining,
                shapeIndex,
                intersectionIndex,
                this
            )
        }
    }
}
