package com.mapbox.navigation.core.telemetry.events

import androidx.annotation.Keep

/**
 * Class that contains step meta data
 */
@Keep
internal class NavigationStepData(metricsRouteProgress: MetricsRouteProgress) {
    val upcomingInstruction: String? = metricsRouteProgress.upcomingStepInstruction // Schema minLength 1
    val upcomingModifier: String? = metricsRouteProgress.upcomingStepModifier
    val upcomingName: String? = metricsRouteProgress.upcomingStepName
    val upcomingType: String? = metricsRouteProgress.upcomingStepType // Schema minLength 1
    val previousInstruction: String? = metricsRouteProgress.previousStepInstruction // Schema minLength 1
    val previousModifier: String? = metricsRouteProgress.previousStepModifier
    val previousName: String? = metricsRouteProgress.previousStepName
    val previousType: String? = metricsRouteProgress.previousStepType // Schema minLength 1
    val distance: Int = metricsRouteProgress.currentStepDistance
    val duration: Int = metricsRouteProgress.currentStepDuration
    val distanceRemaining: Int = metricsRouteProgress.currentStepDistanceRemaining
    val durationRemaining: Int = metricsRouteProgress.currentStepDurationRemaining
}
