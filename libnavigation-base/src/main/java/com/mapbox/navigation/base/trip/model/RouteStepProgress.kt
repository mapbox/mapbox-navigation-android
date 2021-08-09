package com.mapbox.navigation.base.trip.model

import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.geojson.Point

/**
 * This is a progress object specific to the current step the user is on.
 *
 * The latest route step progress object can be obtained through the [RouteProgressObserver].
 * Note that the route step progress object's immutable.
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
class RouteStepProgress private constructor(
    val stepIndex: Int,
    val intersectionIndex: Int,
    val instructionIndex: Int?,
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
    fun toBuilder(): Builder = Builder()
        .stepIndex(stepIndex)
        .intersectionIndex(intersectionIndex)
        .instructionIndex(instructionIndex)
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
        if (intersectionIndex != other.intersectionIndex) return false
        if (instructionIndex != other.instructionIndex) return false
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
        result = 31 * result + intersectionIndex
        result = 31 * result + (instructionIndex ?: 0)
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
        return "RouteStepProgress(" +
            "stepIndex=$stepIndex, " +
            "intersectionIndex=$intersectionIndex, " +
            "instructionIndex=$instructionIndex, " +
            "step=$step, " +
            "stepPoints=$stepPoints, " +
            "distanceRemaining=$distanceRemaining, " +
            "distanceTraveled=$distanceTraveled, " +
            "fractionTraveled=$fractionTraveled, " +
            "durationRemaining=$durationRemaining" +
            ")"
    }

    /**
     * Builder of [RouteStepProgress]
     */
    class Builder {
        private var stepIndex: Int = 0
        private var intersectionIndex: Int = 0
        private var instructionIndex: Int? = null
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
        fun stepIndex(stepIndex: Int): Builder =
            apply { this.stepIndex = stepIndex }

        /**
         * Index representing the current intersection the user is on
         *
         * @return Builder
         */
        fun intersectionIndex(intersectionIndex: Int): Builder =
            apply { this.intersectionIndex = intersectionIndex }

        /**
         * Index of the current instruction in the list of available instructions for this step
         *
         * @return Builder
         */
        fun instructionIndex(instructionIndex: Int?): Builder =
            apply { this.instructionIndex = instructionIndex }

        /**
         * Returns the current step the user is traversing along
         *
         * @return Builder
         */
        fun step(step: LegStep?): Builder =
            apply { this.step = step }

        /**
         * A list of points that represent the current step geometry
         *
         * @return Builder
         */
        fun stepPoints(stepPoints: List<Point>?): Builder =
            apply { this.stepPoints = stepPoints }

        /**
         * Total distance in meters from user to end of step
         *
         * @return Builder
         */
        fun distanceRemaining(distanceRemaining: Float): Builder =
            apply { this.distanceRemaining = distanceRemaining }

        /**
         * Returns distance user has traveled along current step in unit meters
         *
         * @return Builder
         */
        fun distanceTraveled(distanceTraveled: Float): Builder =
            apply { this.distanceTraveled = distanceTraveled }

        /**
         * The fraction traveled along the current step. This is a float value between 0 and 1 and
         * isn't guaranteed to reach 1 before the user reaches the next step (if another step exist in route)
         *
         * @return Builder
         */
        fun fractionTraveled(fractionTraveled: Float): Builder =
            apply { this.fractionTraveled = fractionTraveled }

        /**
         * The duration remaining in seconds until the user reaches the end of the current step
         *
         * @return Builder
         */
        fun durationRemaining(durationRemaining: Double): Builder =
            apply { this.durationRemaining = durationRemaining }

        /**
         * Build new instance of [RouteStepProgress]
         *
         * @return RouteStepProgress
         */
        fun build(): RouteStepProgress {
            return RouteStepProgress(
                stepIndex,
                intersectionIndex,
                instructionIndex,
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
