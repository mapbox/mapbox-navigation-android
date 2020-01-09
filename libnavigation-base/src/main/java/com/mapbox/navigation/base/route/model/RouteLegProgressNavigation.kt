package com.mapbox.navigation.base.route.model

import com.mapbox.geojson.Point
import com.mapbox.navigation.base.extensions.ifNonNull

class RouteLegProgressNavigation private constructor(
    private val stepIndex: Int = 0,
    private val distanceTraveled: Double = 0.0,
    private val distanceRemaining: Double = 0.0,
    private val durationRemaining: Double = 0.0,
    private val fractionTraveled: Float = 0f,
    private val currentStep: LegStepNavigation? = null,
    private val previousStep: LegStepNavigation? = null,
    private val upComingStep: LegStepNavigation? = null,
    private val followOnStep: LegStepNavigation? = null,
    private val currentStepProgress: RouteStepProgressNavigation? = null,
    private val currentStepPoints: List<Point>? = null,
    private val upcomingStepPoints: List<Point>? = null,
    private val routeLeg: RouteLegNavigation? = null,
    private val stepDistanceRemaining: Double = 0.0,
    private val builder: Builder
) {

    /**
     * Index representing the current step the user is on.
     *
     * @return an integer representing the current step the user is on
     */
    fun stepIndex(): Int = stepIndex

    /**
     * Total distance traveled in meters along current leg.
     *
     * @return a double value representing the total distance the user has traveled along the current
     * leg, using unit meters.
     */
    fun distanceTraveled(): Double = distanceTraveled

    /**
     * Provides the duration remaining in seconds till the user reaches the end of the route.
     *
     * @return long value representing the duration remaining till end of route, in unit seconds
     */
    fun distanceRemaining(): Double = distanceRemaining

    /**
     * Provides the duration remaining in seconds till the user reaches the end of the current step.
     *
     * @return long value representing the duration remaining till end of step, in unit seconds.
     */
    fun durationRemaining(): Double = durationRemaining

    /**
     * Get the fraction traveled along the current leg, this is a float value between 0 and 1 and
     * isn't guaranteed to reach 1 before the user reaches the next waypoint.
     *
     * @return a float value between 0 and 1 representing the fraction the user has traveled along the
     * current leg
     */
    fun fractionTraveled(): Float = fractionTraveled

    /**
     * Get the previous step the user traversed along, if the user is still on the first step, this
     * will return null.
     *
     * @return a [LegStepNavigation] representing the previous step the user was on, if still on first
     * step in route, returns null
     */
    fun previousStep(): LegStepNavigation? = previousStep

    /**
     * Returns the current step the user is traversing along.
     *
     * @return a [LegStepNavigation] representing the step the user is currently on
     */
    fun currentStep(): LegStepNavigation? = currentStep

    /**
     * Get the next/upcoming step immediately after the current step. If the user is on the last step
     * on the last leg, this will return null since a next step doesn't exist.
     *
     * @return a [LegStepNavigation] representing the next step the user will be on.
     */
    fun upComingStep(): LegStepNavigation? = upComingStep

    /**
     * This will return the [LegStep] two steps ahead of the current step the user's on. If the
     * user's current step is within 2 steps of their final destination this will return null.
     *
     * @return the [LegStepNavigation] after the [.upComingStep]
     */
    fun followOnStep(): LegStepNavigation? = followOnStep

    /**
     * Gives a [RouteStepProgressNavigation] object with information about the particular step the user
     * is currently on.
     *
     * @return a [RouteStepProgressNavigation] object
     */
    fun currentStepProgress(): RouteStepProgressNavigation? = currentStepProgress

    /**
     * Provides a list of points that represent the current step
     * step geometry.
     *
     * @return list of points representing the current step
     */
    fun currentStepPoints(): List<Point>? = currentStepPoints

    /**
     * Provides a list of points that represent the upcoming step
     * step geometry.
     *
     * @return list of points representing the upcoming step
     */
    fun upcomingStepPoints(): List<Point>? = upcomingStepPoints

    /**
     * Not public since developer can access same information from [RouteProgressNavigation].
     */
    internal fun routeLeg(): RouteLegNavigation? = routeLeg

    internal fun stepDistanceRemaining(): Double = stepDistanceRemaining

    fun toBuilder() = builder

    class Builder {
        private var stepIndex: Int = 0
        private var fractionTraveled: Float = 0f
        private var distanceTraveled: Double = 0.0
        private var distanceRemaining: Double = 0.0
        private var durationRemaining: Double = 0.0
        private var stepDistanceRemaining: Double = 0.0
        private var currentStepPoints: List<Point>? = null
        private var upcomingStepPoints: List<Point>? = null
        private var previousStep: LegStepNavigation? = null
        private var upComingStep: LegStepNavigation? = null
        private var followOnStep: LegStepNavigation? = null
        private lateinit var _routeLeg: RouteLegNavigation
        private lateinit var _currentStep: LegStepNavigation
        private lateinit var currentStepProgress: RouteStepProgressNavigation

        fun stepIndex(stepIndex: Int) = apply { this.stepIndex = stepIndex }

        fun distanceRemaining(distanceRemaining: Double) =
                apply { this.distanceRemaining = distanceRemaining }

        fun durationRemaining(durationRemaining: Double) =
                apply { this.durationRemaining = durationRemaining }

        fun currentStep(currentStep: LegStepNavigation) = apply { this._currentStep = currentStep }

        fun currentStepPoints(currentStepPoints: List<Point>?) =
                apply { this.currentStepPoints = currentStepPoints }

        fun upcomingStepPoints(upcomingStepPoints: List<Point>?) =
                apply { this.upcomingStepPoints = upcomingStepPoints }

        fun routeLeg(routeLeg: RouteLegNavigation) = apply { this._routeLeg = routeLeg }

        fun stepDistanceRemaining(stepDistanceRemaining: Double) =
                apply { this.stepDistanceRemaining = stepDistanceRemaining }

        private fun validate() {
            var missing = ""
            if (!this::_routeLeg.isInitialized) {
                missing += " routeLeg"
            }
            if (!this::_currentStep.isInitialized) {
                missing += " currentStep"
            }
            if (!this::currentStepProgress.isInitialized) {
                missing += " currentStepProgress"
            }
            check(missing.isEmpty()) { "RouteLegProgressNavigation.Builder missing required properties: $missing" }
        }

        fun build(): RouteLegProgressNavigation {
            distanceTraveled = distanceTraveled()
            fractionTraveled = fractionTraveled(distanceTraveled)
            previousStep = previousStep()
            upComingStep = upComingStep()
            followOnStep = followOnStep()
            currentStepProgress = RouteStepProgressNavigation.Builder()
                    .step(_currentStep)
                    .distanceRemaining(stepDistanceRemaining)
                    .build()

            validate()

            return RouteLegProgressNavigation(
                    stepIndex,
                    distanceTraveled,
                    distanceRemaining,
                    durationRemaining,
                    fractionTraveled,
                    _currentStep,
                    previousStep,
                    upComingStep,
                    followOnStep,
                    currentStepProgress,
                    currentStepPoints,
                    upcomingStepPoints,
                    _routeLeg,
                    stepDistanceRemaining,
                    this
            )
        }

        private fun distanceTraveled(): Double =
                _routeLeg.distance()?.let { distance ->
                    return when (distance - distanceRemaining < 0) {
                        true -> {
                            0.0
                        }
                        else -> {
                            distance - distanceRemaining
                        }
                    }
                } ?: distanceRemaining

        private fun fractionTraveled(distanceTraveled: Double): Float {
            if (distanceTraveled == 0.0) {
                return 1.0f
            }
            return _routeLeg.distance()?.let { distance ->
                when (distance > 0) {
                    true -> {
                        (distanceTraveled / distance).toFloat()
                    }
                    else -> {
                        1.0f
                    }
                }
            } ?: 1.0f
        }

        private fun previousStep(): LegStepNavigation? =
                ifNonNull(_routeLeg.steps()) { routeLegSteps ->
                    return when {
                        stepIndex != 0 -> routeLegSteps[stepIndex - 1]
                        else -> null
                    }
                }

        private fun upComingStep(): LegStepNavigation? =
                ifNonNull(_routeLeg.steps()) { routeLegSteps ->
                    return when {
                        routeLegSteps.size - 1 > stepIndex -> routeLegSteps[stepIndex + 1]
                        else -> null
                    }
                }

        private fun followOnStep(): LegStepNavigation? =
                ifNonNull(_routeLeg.steps()) { routeLegSteps ->
                    return when {
                        routeLegSteps.size - 2 > stepIndex -> routeLegSteps[stepIndex + 2]
                        else -> null
                    }
                }
    }

    override fun toString(): String {
        return stepIndex.toString() +
                distanceTraveled.toString() +
                distanceRemaining.toString() +
                fractionTraveled.toString() +
                currentStep.toString() +
                previousStep.toString() +
                upComingStep.toString() +
                followOnStep.toString() +
                currentStepProgress.toString() +
                currentStepPoints.toString() +
                upcomingStepPoints.toString() +
                routeLeg.toString() +
                stepDistanceRemaining.toString()
    }

    override fun equals(other: Any?): Boolean {
        return when (other is RouteLegProgressNavigation) {
            true -> this.toString() == other.toString()
            false -> false
        }
    }
}
