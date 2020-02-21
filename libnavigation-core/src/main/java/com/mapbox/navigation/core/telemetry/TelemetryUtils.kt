package com.mapbox.navigation.core.telemetry

import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.core.telemetry.telemetryevents.TelemetryStep

fun populateTelemetryStep(legProgress: RouteLegProgress): TelemetryStep {
    return TelemetryStep(legProgress.upcomingStep()?.maneuver()?.instruction() ?: "",
            legProgress.upcomingStep()?.maneuver()?.type() ?: "",
            legProgress.upcomingStep()?.maneuver()?.type() ?: "",
            legProgress.upcomingStep()?.name() ?: "",

            legProgress.currentStepProgress()?.step()?.maneuver()?.instruction() ?: "",
            legProgress.currentStepProgress()?.step()?.maneuver()?.type() ?: "",
            legProgress.currentStepProgress()?.step()?.maneuver()?.type() ?: "",
            legProgress.currentStepProgress()?.step()?.name() ?: "",

            legProgress.currentStepProgress()?.distanceTraveled()?.toInt() ?: 0,
            legProgress.currentStepProgress()?.durationRemaining()?.toInt() ?: 0,
            legProgress.upcomingStep()?.distance()?.toInt() ?: 0,
            legProgress.upcomingStep()?.duration()?.toInt() ?: 0
    )
}
