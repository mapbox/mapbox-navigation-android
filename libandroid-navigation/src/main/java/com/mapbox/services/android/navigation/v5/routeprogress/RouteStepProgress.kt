package com.mapbox.services.android.navigation.v5.routeprogress

import com.mapbox.api.directions.v5.models.LegStep

/**
 * This is a progress object specific to the current step the user is on.
 *
 * The latest route step progress object can be obtained through either the [ProgressChangeListener]
 * or the [com.mapbox.services.android.navigation.v5.milestone.MilestoneEventListener] callbacks.
 * Note that the route step progress object's immutable.
 */
data class RouteStepProgress internal constructor(
    var step: LegStep? = null,
    var distanceRemaining: Double? = null,
    var distanceTraveled: Double? = null,
    var fractionTraveled: Float? = null,
    var durationRemaining: Double? = null,
    var builder: Builder
) {

    /**
     * Total distance in meters from user to end of step.
     *
     * @return double value representing the distance the user has remaining till they reach the end
     * of the current step. Uses unit meters.
     */
    fun distanceRemaining(): Double? = distanceRemaining

    /**
     * Returns distance user has traveled along current step in unit meters.
     *
     * @return double value representing the distance the user has traveled so far along the current
     * step. Uses unit meters.
     */
    fun distanceTraveled(): Double? = distanceTraveled

    /**
     * Get the fraction traveled along the current step, this is a float value between 0 and 1 and
     * isn't guaranteed to reach 1 before the user reaches the next step (if another step exist in route).
     *
     * @return a float value between 0 and 1 representing the fraction the user has traveled along
     * the current step.
     */
    fun fractionTraveled(): Float? = fractionTraveled

    /**
     * Provides the duration remaining in seconds till the user reaches the end of the current step.
     *
     * @return `long` value representing the duration remaining till end of step, in unit seconds.
     */
    fun durationRemaining(): Double? = durationRemaining

    internal fun step(): LegStep? = step

    fun toBuilder() = builder

    class Builder {
        private var step: LegStep? = null
        private var distanceRemaining: Double? = null
        private var distanceTraveled: Double? = null
        private var fractionTraveled: Float? = null
        private var durationRemaining: Double? = null

        fun step(step: LegStep) = apply { this.step = step }
        fun distanceRemaining(distanceRemaining: Double) =
            apply { this.distanceRemaining = distanceRemaining }

        fun distanceTraveled(distanceTraveled: Double) =
            apply { this.distanceTraveled = distanceTraveled }

        fun fractionTraveled(fractionTraveled: Float) =
            apply { this.fractionTraveled = fractionTraveled }

        fun durationRemaining(durationRemaining: Double) =
            apply { this.durationRemaining = durationRemaining }

        private fun validate() {
            var missing = ""
            if (this.step == null) {
                missing += " step"
            }
            if (this.distanceRemaining == null) {
                missing += " distanceRemaining"
            }
            if (this.distanceTraveled == null) {
                missing += " distanceTraveled"
            }
            if (this.fractionTraveled == null) {
                missing += " fractionTraveled"
            }
            if (this.durationRemaining == null) {
                missing += " durationRemaining"
            }
            check(missing.isEmpty()) { "Missing required properties: $missing" }
        }

        fun build(): RouteStepProgress {
            distanceTraveled = calculateDistanceTraveled(step, distanceRemaining)
            fractionTraveled = calculateFractionTraveled(step, distanceTraveled)
            durationRemaining = calculateDurationRemaining(step, fractionTraveled)

            validate()

            return RouteStepProgress(
                step,
                distanceRemaining,
                distanceTraveled,
                fractionTraveled,
                durationRemaining,
                this
            )
        }

        private fun calculateDistanceTraveled(step: LegStep?, distanceRemaining: Double?): Double {
            val currentDistance = step?.distance() ?: 0.0
            val _distanceRemaining = distanceRemaining ?: 0.0

            val distanceTraveled = currentDistance - _distanceRemaining
            return when {
                distanceTraveled < 0 -> 0.0
                else -> distanceTraveled
            }
        }

        private fun calculateFractionTraveled(step: LegStep?, distanceTraveled: Double?): Float {
            val currentDistance = step?.distance() ?: 0.0
            when {
                currentDistance <= 0 -> return 1f
                else -> {
                    val _distanceTraveled = distanceTraveled ?: 0.0
                    val fractionTraveled = (_distanceTraveled / currentDistance).toFloat()
                    return when {
                        fractionTraveled < 0 -> return 0f
                        else -> fractionTraveled
                    }
                }
            }
        }

        private fun calculateDurationRemaining(step: LegStep?, fractionTraveled: Float?): Double {
            val currentDuration = step?.duration() ?: 0.0
            val _fractionTraveled = fractionTraveled ?: 0f
            return (1 - _fractionTraveled) * currentDuration
        }
    }
}
