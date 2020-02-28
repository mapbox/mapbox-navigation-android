package com.mapbox.navigation.core.telemetry.telemetryevents

import com.mapbox.navigation.base.trip.model.RouteLegProgress

/**
 * Documentation is here [https://paper.dropbox.com/doc/Navigation-Telemetry-Events-V1--AuUz~~~rEVK7iNB3dQ4_tF97Ag-iid3ZImnt4dsW7Z6zC3Lc]
 */

// Defaulted values are optional

data class TelemetryStep(
    val upcomingInstruction: String,
    val upcomingType: String,
    val upcomingModifier: String,
    val upcomingName: String,
    val previousInstruction: String,
    val previousType: String,
    val previousModifier: String,
    val previousName: String,
    val distance: Int,
    val duration: Int,
    val distanceRemaining: Int,
    val durationRemaining: Int
)

fun TelemetryStep.populateTelemetryStep(legProgress: RouteLegProgress): TelemetryStep = apply {

    legProgress.upcomingStep()?.maneuver()?.instruction()
    legProgress.upcomingStep()?.maneuver()?.type()
    legProgress.upcomingStep()?.maneuver()?.type()
    legProgress.upcomingStep()?.name()

    legProgress.currentStepProgress()?.step()?.maneuver()?.instruction()
    legProgress.currentStepProgress()?.step()?.maneuver()?.type()
    legProgress.currentStepProgress()?.step()?.maneuver()?.type()
    legProgress.currentStepProgress()?.step()?.name()

    legProgress.currentStepProgress()?.distanceTraveled()?.toInt()
    legProgress.currentStepProgress()?.durationRemaining()?.toInt()
    legProgress.upcomingStep()?.distance()?.toInt()
    legProgress.upcomingStep()?.duration()?.toInt()
    return this
}
