package com.mapbox.navigation.core.telemetry.events

/**
 * Class that contains step meta data
 */
internal class NavigationStepData(metricsRouteProgress: MetricsRouteProgress) {
    val upcomingInstruction: String? =
        metricsRouteProgress.upcomingStepInstruction // Schema minLength 1
    val upcomingModifier: String? = metricsRouteProgress.upcomingStepModifier
    val upcomingName: String? = metricsRouteProgress.upcomingStepName
    val upcomingType: String? = metricsRouteProgress.upcomingStepType // Schema minLength 1
    val previousInstruction: String? =
        metricsRouteProgress.previousStepInstruction // Schema minLength 1
    val previousModifier: String? = metricsRouteProgress.previousStepModifier
    val previousName: String? = metricsRouteProgress.previousStepName
    val previousType: String? = metricsRouteProgress.previousStepType // Schema minLength 1
    val distance: Int = metricsRouteProgress.currentStepDistance
    val duration: Int = metricsRouteProgress.currentStepDuration
    val distanceRemaining: Int = metricsRouteProgress.currentStepDistanceRemaining
    val durationRemaining: Int = metricsRouteProgress.currentStepDurationRemaining

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NavigationStepData

        if (upcomingInstruction != other.upcomingInstruction) return false
        if (upcomingModifier != other.upcomingModifier) return false
        if (upcomingName != other.upcomingName) return false
        if (upcomingType != other.upcomingType) return false
        if (previousInstruction != other.previousInstruction) return false
        if (previousModifier != other.previousModifier) return false
        if (previousName != other.previousName) return false
        if (previousType != other.previousType) return false
        if (distance != other.distance) return false
        if (duration != other.duration) return false
        if (distanceRemaining != other.distanceRemaining) return false
        if (durationRemaining != other.durationRemaining) return false

        return true
    }

    override fun hashCode(): Int {
        var result = upcomingInstruction?.hashCode() ?: 0
        result = 31 * result + (upcomingModifier?.hashCode() ?: 0)
        result = 31 * result + (upcomingName?.hashCode() ?: 0)
        result = 31 * result + (upcomingType?.hashCode() ?: 0)
        result = 31 * result + (previousInstruction?.hashCode() ?: 0)
        result = 31 * result + (previousModifier?.hashCode() ?: 0)
        result = 31 * result + (previousName?.hashCode() ?: 0)
        result = 31 * result + (previousType?.hashCode() ?: 0)
        result = 31 * result + distance
        result = 31 * result + duration
        result = 31 * result + distanceRemaining
        result = 31 * result + durationRemaining
        return result
    }
}
