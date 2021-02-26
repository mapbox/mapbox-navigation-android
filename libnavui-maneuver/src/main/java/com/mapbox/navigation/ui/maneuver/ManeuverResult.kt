package com.mapbox.navigation.ui.maneuver

import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.navigation.ui.maneuver.model.Maneuver

internal sealed class ManeuverResult {

    data class GetManeuver(
        val maneuver: Maneuver
    ) : ManeuverResult()

    data class GetStepDistanceRemaining(
        val distanceRemaining: Double
    ) : ManeuverResult()

    data class GetAllBannerInstructions(
        val bannerInstructions: List<BannerInstructions>
    ) : ManeuverResult()

    data class GetAllBannerInstructionsAfterStep(
        val bannerInstructions: List<BannerInstructions>
    ) : ManeuverResult()

    data class GetAllManeuvers(
        val maneuverList: List<Maneuver>
    ) : ManeuverResult()
}
