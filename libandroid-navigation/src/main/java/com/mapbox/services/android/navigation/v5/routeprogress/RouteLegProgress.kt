package com.mapbox.services.android.navigation.v5.routeprogress

import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.geojson.Point
import com.mapbox.services.android.navigation.v5.utils.extensions.ifNonNull

/**
 * This is a progress object specific to the current leg the user is on. If there is only one leg
 * in the directions route, much of this information will be identical to the parent
 * [RouteProgress].
 *
 * The latest route leg progress object can be obtained through either the [ProgressChangeListener]
 * or the [com.mapbox.services.android.navigation.v5.milestone.MilestoneEventListener] callbacks.
 * Note that the route leg progress object's immutable.
 */
data class RouteLegProgress internal constructor(
    var stepIndex: Int? = null,
    var distanceTraveled: Double? = null,
    var distanceRemaining: Double? = null,
    var durationRemaining: Double? = null,
    var fractionTraveled: Float? = null,
    var previousStep: LegStep? = null,
    var currentStep: LegStep? = null,
    var upComingStep: LegStep? = null,
    var followOnStep: LegStep? = null,
    var currentStepProgress: RouteStepProgress? = null,
    var currentStepPoints: List<Point>? = null,
    var upcomingStepPoints: List<Point>? = null,
    var routeLeg: RouteLeg? = null,
    var stepDistanceRemaining: Double? = null,
    var builder: Builder
) {

    /**
     * Index representing the current step the user is on.
     *
     * @return an integer representing the current step the user is on
     */
    fun stepIndex(): Int? = stepIndex

    /**
     * Total distance traveled in meters along current leg.
     *
     * @return a double value representing the total distance the user has traveled along the current
     * leg, using unit meters.
     */
    fun distanceTraveled(): Double? = distanceTraveled

    /**
     * Provides the duration remaining in seconds till the user reaches the end of the route.
     *
     * @return long value representing the duration remaining till end of route, in unit seconds
     */
    fun distanceRemaining(): Double? = distanceRemaining

    /**
     * Provides the duration remaining in seconds till the user reaches the end of the current step.
     *
     * @return long value representing the duration remaining till end of step, in unit seconds.
     */
    fun durationRemaining(): Double? = durationRemaining

    /**
     * Get the fraction traveled along the current leg, this is a float value between 0 and 1 and
     * isn't guaranteed to reach 1 before the user reaches the next waypoint.
     *
     * @return a float value between 0 and 1 representing the fraction the user has traveled along the
     * current leg
     */
    fun fractionTraveled(): Float? = fractionTraveled

    /**
     * Get the previous step the user traversed along, if the user is still on the first step, this
     * will return null.
     *
     * @return a [LegStep] representing the previous step the user was on, if still on first
     * step in route, returns null
     */
    fun previousStep(): LegStep? = previousStep

    /**
     * Returns the current step the user is traversing along.
     *
     * @return a [LegStep] representing the step the user is currently on
     */
    fun currentStep(): LegStep? = currentStep

    /**
     * Get the next/upcoming step immediately after the current step. If the user is on the last step
     * on the last leg, this will return null since a next step doesn't exist.
     *
     * @return a [LegStep] representing the next step the user will be on.
     */
    fun upComingStep(): LegStep? = upComingStep

    /**
     * This will return the [LegStep] two steps ahead of the current step the user's on. If the
     * user's current step is within 2 steps of their final destination this will return null.
     *
     * @return the [LegStep] after the [.upComingStep]
     */
    fun followOnStep(): LegStep? = followOnStep

    /**
     * Gives a [RouteStepProgress] object with information about the particular step the user
     * is currently on.
     *
     * @return a [RouteStepProgress] object
     */
    fun currentStepProgress(): RouteStepProgress? = currentStepProgress

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
     * Not public since developer can access same information from [RouteProgress].
     */
    internal fun routeLeg(): RouteLeg? = routeLeg

    internal fun stepDistanceRemaining(): Double? = stepDistanceRemaining

    fun toBuilder() = builder

    class Builder {
        private var stepIndex: Int? = null
        private var distanceTraveled: Double? = null
        private var distanceRemaining: Double? = null
        private var durationRemaining: Double? = null
        private var fractionTraveled: Float? = null
        private var previousStep: LegStep? = null
        private var currentStep: LegStep? = null
        private var upComingStep: LegStep? = null
        private var followOnStep: LegStep? = null
        private var currentStepProgress: RouteStepProgress? = null
        private var currentStepPoints: List<Point>? = null
        private var upcomingStepPoints: List<Point>? = null
        private var routeLeg: RouteLeg? = null
        private var stepDistanceRemaining: Double? = null

        fun stepIndex(stepIndex: Int) = apply { this.stepIndex = stepIndex }
        fun distanceTraveled(distanceTraveled: Double) =
            apply { this.distanceTraveled = distanceTraveled }

        fun distanceRemaining(distanceRemaining: Double) =
            apply { this.distanceRemaining = distanceRemaining }

        fun durationRemaining(durationRemaining: Double) =
            apply { this.durationRemaining = durationRemaining }

        fun fractionTraveled(fractionTraveled: Float) =
            apply { this.fractionTraveled = fractionTraveled }

        fun previousStep(previousStep: LegStep) = apply { this.previousStep = previousStep }
        fun currentStep(currentStep: LegStep) = apply { this.currentStep = currentStep }
        fun upComingStep(upComingStep: LegStep) = apply { this.upComingStep = upComingStep }
        fun followOnStep(followOnStep: LegStep) = apply { this.followOnStep = followOnStep }
        fun currentStepProgress(currentStepProgress: RouteStepProgress) =
            apply { this.currentStepProgress = currentStepProgress }

        fun currentStepPoints(currentStepPoints: List<Point>?) =
            apply { this.currentStepPoints = currentStepPoints }

        fun upcomingStepPoints(upcomingStepPoints: List<Point>?) =
            apply { this.upcomingStepPoints = upcomingStepPoints }

        fun routeLeg(routeLeg: RouteLeg) = apply { this.routeLeg = routeLeg }
        fun stepDistanceRemaining(stepDistanceRemaining: Double) =
            apply { this.stepDistanceRemaining = stepDistanceRemaining }

        private fun validate() {
            var missing = ""
            if (this.stepIndex == null) {
                missing += " stepIndex"
            }
            if (this.distanceTraveled == null) {
                missing += " distanceTraveled"
            }
            if (this.distanceRemaining == null) {
                missing += " distanceRemaining"
            }
            if (this.durationRemaining == null) {
                missing += " durationRemaining"
            }
            if (this.fractionTraveled == null) {
                missing += " fractionTraveled"
            }
            if (this.currentStep == null) {
                missing += " currentStep"
            }
            if (this.currentStepProgress == null) {
                missing += " currentStepProgress"
            }
            if (this.stepDistanceRemaining == null) {
                missing += " stepDistanceRemaining"
            }
            check(missing.isEmpty()) { "Missing required properties: $missing" }
        }

        fun build(): RouteLegProgress {
            distanceTraveled = distanceTraveled()
            fractionTraveled = fractionTraveled()
            previousStep = previousStep()
            upComingStep = upComingStep()
            followOnStep = followOnStep()
            currentStepProgress = RouteStepProgress.Builder()
                .step(currentStep!!)
                .distanceRemaining(stepDistanceRemaining!!)
                .build()

            validate()

            return RouteLegProgress(
                stepIndex,
                distanceTraveled,
                distanceRemaining,
                durationRemaining,
                fractionTraveled,
                previousStep,
                currentStep,
                upComingStep,
                followOnStep,
                currentStepProgress,
                currentStepPoints,
                upcomingStepPoints,
                routeLeg,
                stepDistanceRemaining,
                this
            )
        }

        private fun distanceTraveled(): Double? =
            ifNonNull(routeLeg?.distance(), distanceRemaining) { distance, distanceRemaining ->
                when (distance - distanceRemaining < 0) {
                    true -> {
                        0.0
                    }
                    false -> {
                        distance - distanceRemaining
                    }
                }
            }

        private fun fractionTraveled(): Float? =
            ifNonNull(distanceTraveled(), routeLeg?.distance()) { distanceTraveled, distance ->
                when (distance > 0) {
                    true -> {
                        (distanceTraveled / distance).toFloat()
                    }
                    false -> {
                        1.0f
                    }
                }
            } ?: 1.0f

        private fun previousStep(): LegStep? =
            ifNonNull(routeLeg?.steps(), stepIndex) { routeLegSteps, stepIndex ->
                return when {
                    stepIndex != 0 -> routeLegSteps[stepIndex - 1]
                    else -> null
                }
            }

        private fun upComingStep(): LegStep? =
            ifNonNull(routeLeg?.steps(), stepIndex) { routeLegSteps, stepIndex ->
                return when {
                    routeLegSteps.size - 1 > stepIndex -> routeLegSteps[stepIndex + 1]
                    else -> null
                }
            }

        private fun followOnStep(): LegStep? =
            ifNonNull(routeLeg?.steps(), stepIndex) { routeLegSteps, stepIndex ->
                return when {
                    routeLegSteps.size - 2 > stepIndex -> routeLegSteps[stepIndex + 2]
                    else -> null
                }
            }
    }
}
