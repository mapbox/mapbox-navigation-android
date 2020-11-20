package com.mapbox.navigation.ui.maps.guidance.internal

import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.navigation.ui.base.MapboxAction

sealed class GuidanceImageAction: MapboxAction {
    data class GuidanceImageAvailable(val bannerInstruction: BannerInstructions) : GuidanceImageAction()
    data class ShouldShowUrlBasedGuidance(val bannerComponent: BannerComponents) : GuidanceImageAction()
    data class ShouldShowSnapshotBasedGuidance(val bannerComponent: BannerComponents) : GuidanceImageAction()
}
