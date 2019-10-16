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
 *
 * The latest route leg progress object can be obtained through either the [ProgressChangeListener]
 * or the [com.mapbox.services.android.navigation.v5.milestone.MilestoneEventListener] callbacks.
 * Note that the route leg progress object's immutable.
 *
 *
 * @since 0.1.0
 */
data class RouteLegProgress(
    var stepIndex: Int?,
    var distanceRemaining: Double?,
    var durationRemaining: Double?,
    var currentStep: LegStep?,
    var currentStepProgress: RouteStepProgress?,
    var currentStepPoints: List<Point>?,
    var upcomingStepPoints: List<Point>? = null,
    var routeLeg: RouteLeg?,
    var stepDistanceRemaining: Double?
) {

    /**
     * Index representing the current step the user is on.
     *
     * @return an integer representing the current step the user is on
     * @since 0.1.0
     */
    fun stepIndex() = stepIndex

    /**
     * Total distance traveled in meters along current leg.
     *
     * @return a double value representing the total distance the user has traveled along the current
     * leg, using unit meters.
     * @since 0.1.0
     */
    fun distanceTraveled() =
            ifNonNull(routeLeg()?.distance(), distanceRemaining()) { distance, distanceRemaining ->
                var distanceTraveled = distance - distanceRemaining
                when (distanceTraveled < 0) {
                    true -> {
                        distanceTraveled = 0.0
                    }
                    false -> {
                        Unit
                    }
                }
                distanceTraveled
            } ?: 0.0

    /**
     * Provides the duration remaining in seconds till the user reaches the end of the route.
     *
     * @return long value representing the duration remaining till end of route, in unit seconds
     * @since 0.1.0
     */
    fun distanceRemaining() = distanceRemaining

    /**
     * Provides the duration remaining in seconds till the user reaches the end of the current step.
     *
     * @return long value representing the duration remaining till end of step, in unit seconds.
     * @since 0.1.0
     */
    fun durationRemaining() = durationRemaining

    /**
     * Get the fraction traveled along the current leg, this is a float value between 0 and 1 and
     * isn't guaranteed to reach 1 before the user reaches the next waypoint.
     *
     * @return a float value between 0 and 1 representing the fraction the user has traveled along the
     * current leg
     * @since 0.1.0
     */
    fun fractionTraveled(): Float {
        var fractionTraveled = 1f

        ifNonNull(routeLeg()?.distance()) { distance ->
            if (distance > 0.0F) {
                fractionTraveled = (distanceTraveled() / distance).toFloat()
                if (fractionTraveled < 0) {
                    fractionTraveled = 0f
                }
            }
        }
        return fractionTraveled
    }

    /**
     * Get the previous step the user traversed along, if the user is still on the first step, this
     * will return null.
     *
     * @return a [LegStep] representing the previous step the user was on, if still on first
     * step in route, returns null
     * @since 0.1.0
     */
    fun previousStep() = when (stepIndex() == 0) {
        true -> {
            null
        }
        else -> {
            ifNonNull(routeLeg()?.steps(), stepIndex()) { steps, stepIndex ->
                steps[stepIndex - 1]
            }
        }
    }

    /**
     * Returns the current step the user is traversing along.
     *
     * @return a [LegStep] representing the step the user is currently on
     * @since 0.1.0
     */
    fun currentStep() = currentStep

    /**
     * Get the next/upcoming step immediately after the current step. If the user is on the last step
     * on the last leg, this will return null since a next step doesn't exist.
     *
     * @return a [LegStep] representing the next step the user will be on.
     * @since 0.1.0
     */
    fun upComingStep() = ifNonNull(routeLeg()?.steps(), stepIndex()) { steps, stepIndex ->
        if (steps.size - 1 > stepIndex) {
            steps[stepIndex + 1]
        } else null
    }

    /**
     * This will return the [LegStep] two steps ahead of the current step the user's on. If the
     * user's current step is within 2 steps of their final destination this will return null.
     *
     * @return the [LegStep] after the [.upComingStep]
     * @since 0.5.0
     */
    fun followOnStep() = ifNonNull(routeLeg()?.steps(), stepIndex()) { steps, stepIndex ->
        if (steps.size - 2 > stepIndex) {
            steps[stepIndex + 2]
        } else null
    }

    /**
     * Gives a [RouteStepProgress] object with information about the particular step the user
     * is currently on.
     *
     * @return a [RouteStepProgress] object
     * @since 0.1.0
     */
    fun currentStepProgress() = currentStepProgress

    /**
     * Provides a list of points that represent the current step
     * step geometry.
     *
     * @return list of points representing the current step
     * @since 0.12.0
     */
    fun currentStepPoints() = currentStepPoints

    /**
     * Provides a list of points that represent the upcoming step
     * step geometry.
     *
     * @return list of points representing the upcoming step
     * @since 0.12.0
     */
    fun upcomingStepPoints() = upcomingStepPoints

    /**
     * Not public since developer can access same information from [RouteProgress].
     */
    internal fun routeLeg() = routeLeg

    internal fun stepDistanceRemaining() = stepDistanceRemaining

    class Builder {
        var stepIndex: Int? = null
        var distanceRemaining: Double? = null
        var durationRemaining: Double? = null
        var currentStep: LegStep? = null
        var currentStepProgress: RouteStepProgress? = null
        var currentStepPoints: List<Point>? = null
        var upcomingStepPoints: List<Point>? = null
        var routeLeg: RouteLeg? = null
        var stepDistanceRemaining: Double? = null

        fun routeLeg(routeLeg: RouteLeg) = apply { this.routeLeg = routeLeg }

        fun currentStep(currentStep: LegStep) = apply { this.currentStep = currentStep }

        fun currentStep() = apply { this.currentStep = currentStep }

        fun stepIndex(stepIndex: Int) = apply { this.stepIndex = stepIndex }

        fun durationRemaining(durationRemaining: Double) = apply { this.durationRemaining = durationRemaining }

        fun stepDistanceRemaining(stepDistanceRemaining: Double) = apply { this.stepDistanceRemaining = stepDistanceRemaining }

        fun stepDistanceRemaining() = apply { this.stepDistanceRemaining = stepDistanceRemaining }

        fun distanceRemaining(distanceRemaining: Double) = apply { this.distanceRemaining = distanceRemaining }

        fun currentStepProgress(routeStepProgress: RouteStepProgress?) = apply { this.currentStep = currentStep }

        fun currentStepPoints(currentStepPoints: List<Point>) = apply { this.currentStepPoints = currentStepPoints }

        fun upcomingStepPoints(upcomingStepPoints: List<Point>?) = apply { this.upcomingStepPoints = upcomingStepPoints }

        fun build(): RouteLegProgress {
            val stepProgress = ifNonNull(currentStep, stepDistanceRemaining) { currentStep, stepDistanceRemaining ->
                RouteStepProgress.builder()
                        .step(currentStep)
                        .distanceRemaining(stepDistanceRemaining)
                        .build()
            }
            currentStepProgress(stepProgress)
            return RouteLegProgress(stepIndex,
                    distanceRemaining,
                    durationRemaining,
                    currentStep,
                    currentStepProgress,
                    currentStepPoints,
                    upcomingStepPoints,
                    routeLeg,
                    stepDistanceRemaining
            )
        }
    }
}
