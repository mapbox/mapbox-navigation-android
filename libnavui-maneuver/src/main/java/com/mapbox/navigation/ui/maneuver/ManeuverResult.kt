package com.mapbox.navigation.ui.maneuver

import com.mapbox.navigation.ui.base.model.maneuver.Maneuver

internal sealed class ManeuverResult {

    data class CurrentManeuver(
        val currentManeuver: Maneuver
    ) : ManeuverResult()

    data class StepDistanceRemaining(
        val distanceRemaining: Double
    ) : ManeuverResult()

    data class UpcomingManeuvers(
        val upcomingManeuverList: List<Maneuver>
    ) : ManeuverResult()
}
