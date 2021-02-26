package com.mapbox.navigation.ui.maneuver

import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteStepProgress

internal sealed class ManeuverAction {

    data class GetManeuver(
        val bannerInstruction: BannerInstructions
    ) : ManeuverAction()

    data class GetStepDistanceRemaining(
        val stepProgress: RouteStepProgress
    ) : ManeuverAction()

    data class GetAllBannerInstructions(
        val routeProgress: RouteProgress
    ) : ManeuverAction()

    data class GetAllBannerInstructionsAfterStep(
        val routeProgress: RouteProgress,
        val bannerInstructions: List<BannerInstructions>
    ) : ManeuverAction()

    data class GetAllManeuvers(
        val bannerInstructions: List<BannerInstructions>
    ) : ManeuverAction()
}
