package com.mapbox.navigation.ui.maps.guidance.internal

import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.navigation.ui.base.MapboxResult

sealed class GuidanceImageResult: MapboxResult {
    data class GuidanceImageAvailable(val bannerComponent: BannerComponents?): GuidanceImageResult()
    data class ShouldShowUrlBasedGuidance(val isUrlBased: Boolean): GuidanceImageResult()
    data class ShouldShowSnapshotBasedGuidance(val isSnapshotBased: Boolean): GuidanceImageResult()
}
