package com.mapbox.navigation.ui.maps.guidance.internal

import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.navigation.ui.base.MapboxProcessor
import com.mapbox.navigation.ui.base.domain.BannerInstructionsApi

object GuidanceImageProcessor: MapboxProcessor<GuidanceImageAction, GuidanceImageResult>, BannerInstructionsApi {

    override fun process(action: GuidanceImageAction): GuidanceImageResult {
        return when(action) {
            is GuidanceImageAction.GuidanceImageAvailable -> {
                GuidanceImageResult.GuidanceImageAvailable(
                    getComponentWithGuidanceImage(action.bannerInstruction)
                )
            }
            is GuidanceImageAction.ShouldShowUrlBasedGuidance -> {
                GuidanceImageResult.ShouldShowUrlBasedGuidance(
                    shouldShowUrlBasedGuidance(action.bannerComponent)
                )
            }
            is GuidanceImageAction.ShouldShowSnapshotBasedGuidance -> {
                GuidanceImageResult.ShouldShowSnapshotBasedGuidance(
                    shouldShowSnapshotBasedGuidance(action.bannerComponent)
                )
            }
        }
    }

    private fun getComponentWithGuidanceImage(bannerInstructions: BannerInstructions): BannerComponents? {
        val bannerComponents = getBannerComponents(bannerInstructions)
        return when {
            bannerComponents != null -> {
                findTypeGuidanceView(bannerComponents)
            }
            else -> {
                null
            }
        }
    }

    private fun findTypeGuidanceView(componentList: MutableList<BannerComponents>): BannerComponents? {
        return componentList.find { it.type() == BannerComponents.GUIDANCE_VIEW }
    }

    private fun shouldShowUrlBasedGuidance(component: BannerComponents): Boolean =
        //component.subType() == BannerComponents.URL
        false


    private fun shouldShowSnapshotBasedGuidance(component: BannerComponents): Boolean =
        //component.subType() == BannerComponents.SIGNBOARD
        true
}
