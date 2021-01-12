package com.mapbox.navigation.ui.maneuver

import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.navigation.base.trip.model.RouteStepProgress

internal sealed class ManeuverAction {

    data class ParseCurrentManeuver(
        val bannerInstruction: BannerInstructions
    ) : ManeuverAction()

    data class FindStepDistanceRemaining(
        val stepProgress: RouteStepProgress
    ) : ManeuverAction()

    data class FindAllUpcomingManeuvers(
        val routeLeg: RouteLeg
    ) : ManeuverAction()
}
