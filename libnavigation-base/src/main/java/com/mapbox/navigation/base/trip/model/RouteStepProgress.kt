package com.mapbox.navigation.base.trip.model

import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.geojson.Point

/**
 * This is a progress object specific to the current step the user is on.
 *
 * The latest route step progress object can be obtained through the [com.mapbox.navigation.core.trip.session.RouteProgressObserver].
 * Note that the route step progress object's immutable.
 *
 * @param stepIndex [Int] Index representing the current step the user is on
 * @param step [LegStep] Returns the current step the user is traversing along
 * @param stepPoints [List][Point] that represent the current step geometry
 * @param distanceRemaining [Float] Total distance in meters from user to end of step
 * @param distanceTraveled [Float] Distance user has traveled along current step in unit meters
 * @param fractionTraveled [Float] The fraction traveled along the current step, this is a
 * float value between 0 and 1 and isn't guaranteed to reach 1 before the user reaches the
 * next step (if another step exist in route)
 * @param durationRemaining [Double] The duration remaining in seconds until the user reaches the end of the current step
 */
class RouteStepProgress private constructor(
    val stepIndex: Int,
    val step: LegStep?,
    val stepPoints: List<Point>?,
    val distanceRemaining: Float,
    val distanceTraveled: Float,
    val fractionTraveled: Float,
    val durationRemaining: Double
) {

    /**
     * @return builder matching the one used to create this instance
     */
    fun toBuilder() = Builder()
        .stepIndex(stepIndex)
        .step(step)
        .stepPoints(stepPoints)
        .distanceRemaining(distanceRemaining)
        .distanceTraveled(distanceTraveled)
        .fractionTraveled(fractionTraveled)
        .durationRemaining(durationRemaining)

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RouteStepProgress

        if (stepIndex != other.stepIndex) return false
        if (step != other.step) return false
        if (stepPoints != other.stepPoints) return false
        if (distanceRemaining != other.distanceRemaining) return false
        if (distanceTraveled != other.distanceTraveled) return false
        if (fractionTraveled != other.fractionTraveled) return false
        if (durationRemaining != other.durationRemaining) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = stepIndex
        result = 31 * result + (step?.hashCode() ?: 0)
        result = 31 * result + (stepPoints?.hashCode() ?: 0)
        result = 31 * result + distanceRemaining.hashCode()
        result = 31 * result + distanceTraveled.hashCode()
        result = 31 * result + fractionTraveled.hashCode()
        result = 31 * result + durationRemaining.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "RouteStepProgress(stepIndex=$stepIndex, step=$step, stepPoints=$stepPoints, distanceRemaining=$distanceRemaining, distanceTraveled=$distanceTraveled, fractionTraveled=$fractionTraveled, durationRemaining=$durationRemaining)"
    }

    /**
     * Builder of [RouteStepProgress]
     */
    class Builder {
        private var stepIndex: Int = 0
        private var step: LegStep? = null
        private var stepPoints: List<Point>? = null
        private var distanceRemaining: Float = 0f
        private var distanceTraveled: Float = 0f
        private var fractionTraveled: Float = 0f
        private var durationRemaining: Double = 0.0

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
        fun step(step: LegStep?) =
            apply { this.step = step }

        /**
         * A list of points that represent the current step geometry
         *
         * @return Builder
         */
        fun stepPoints(stepPoints: List<Point>?) =
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
                durationRemaining
            )
        }
    }
}
