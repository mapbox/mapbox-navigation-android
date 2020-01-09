package com.mapbox.navigation.base.route.model

class RouteStepProgressNavigation private constructor(
    private val step: LegStepNavigation? = null,
    private val distanceRemaining: Double = 0.0,
    private val distanceTraveled: Double = 0.0,
    private val fractionTraveled: Float = 0f,
    private val durationRemaining: Double = 0.0,
    private val builder: Builder
) {

    /**
     * Total distance in meters from user to end of step.
     *
     * @return double value representing the distance the user has remaining till they reach the end
     * of the current step. Uses unit meters.
     */
    fun distanceRemaining(): Double = distanceRemaining

    /**
     * Returns distance user has traveled along current step in unit meters.
     *
     * @return double value representing the distance the user has traveled so far along the current
     * step. Uses unit meters.
     */
    fun distanceTraveled(): Double = distanceTraveled

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
     * @return `long` value representing the duration remaining till end of step, in unit seconds.
     */
    fun durationRemaining(): Double = durationRemaining

    /**
     * Provides data about one [LegStepNavigation] representing the step the user is on
     *
     * @return a [LegStepNavigation]
     */
    fun step(): LegStepNavigation? = step

    fun toBuilder() = builder

    class Builder {
        private lateinit var legStep: LegStepNavigation
        private var distanceRemaining: Double = 0.0
        private var distanceTraveled: Double = 0.0
        private var fractionTraveled: Float = 0f
        private var durationRemaining: Double = 0.0

        fun step(step: LegStepNavigation) = apply { this.legStep = step }

        fun distanceRemaining(distanceRemaining: Double) =
                apply { this.distanceRemaining = distanceRemaining }

        private fun validate() {
            var missing = ""
            if (!this::legStep.isInitialized) {
                missing += " legStep"
            }
            check(missing.isEmpty()) { "RouteStepProgressNavigation.Builder missing required properties: $missing" }
        }

        fun build(): RouteStepProgressNavigation {
            distanceTraveled = calculateDistanceTraveled(legStep, distanceRemaining)
            fractionTraveled = calculateFractionTraveled(legStep, distanceTraveled)
            durationRemaining = calculateDurationRemaining(legStep, fractionTraveled)

            validate()

            return RouteStepProgressNavigation(
                    legStep,
                    distanceRemaining,
                    distanceTraveled,
                    fractionTraveled,
                    durationRemaining,
                    this
            )
        }

        private fun calculateDistanceTraveled(step: LegStepNavigation, distanceRemaining: Double): Double {
            val distanceTraveled = step.distance() - distanceRemaining
            return when {
                distanceTraveled < 0 -> 0.0
                else -> distanceTraveled
            }
        }

        private fun calculateFractionTraveled(step: LegStepNavigation, distanceTraveled: Double): Float {
            val currentDistance = step.distance()
            when {
                currentDistance <= 0 -> return 1f
                else -> {
                    val fractionTraveled = (distanceTraveled / currentDistance).toFloat()
                    return when {
                        fractionTraveled < 0 -> return 0f
                        else -> fractionTraveled
                    }
                }
            }
        }

        private fun calculateDurationRemaining(
            step: LegStepNavigation,
            fractionTraveled: Float
        ): Double = (1 - fractionTraveled) * step.duration()
    }

    override fun toString(): String {
        return this.step.toString() +
                this.distanceRemaining.toString() +
                this.distanceTraveled.toString() +
                this.fractionTraveled.toString() +
                this.durationRemaining.toString()
    }

    override fun equals(other: Any?): Boolean {
        return when (other is RouteStepProgressNavigation) {
            true -> this.toString() == other.toString()
            false -> false
        }
    }
}
