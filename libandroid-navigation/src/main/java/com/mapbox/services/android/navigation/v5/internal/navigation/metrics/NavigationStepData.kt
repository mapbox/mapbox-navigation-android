package com.mapbox.services.android.navigation.v5.internal.navigation.metrics

import com.mapbox.services.android.navigation.v5.internal.navigation.routeprogress.MetricsRouteProgress

/**
 * Class that contains step meta data
 */
internal class NavigationStepData(metricsRouteProgress: MetricsRouteProgress) {
    val upcomingInstruction: String? = metricsRouteProgress.upcomingStepInstruction
    val upcomingModifier: String? = metricsRouteProgress.upcomingStepModifier
    val upcomingName: String? = metricsRouteProgress.upcomingStepName
    val upcomingType: String? = metricsRouteProgress.upcomingStepType
    val previousInstruction: String? = metricsRouteProgress.previousStepInstruction
    val previousModifier: String? = metricsRouteProgress.previousStepModifier
    val previousName: String? = metricsRouteProgress.previousStepName
    val previousType: String? = metricsRouteProgress.previousStepType
    val distance: Int = metricsRouteProgress.currentStepDistance
    val duration: Int = metricsRouteProgress.currentStepDuration
    val distanceRemaining: Int = metricsRouteProgress.currentStepDistanceRemaining
    val durationRemaining: Int = metricsRouteProgress.currentStepDurationRemaining
}
