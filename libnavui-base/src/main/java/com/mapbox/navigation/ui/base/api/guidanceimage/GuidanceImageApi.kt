package com.mapbox.navigation.ui.base.api.guidanceimage

import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.navigation.base.trip.model.RouteProgress

interface GuidanceImageApi {

    /**
     * It determines if an intersection has a junction given [BannerInstructions]
     * @param instruction BannerInstructions The object representing instruction for a maneuver.
     */
    fun generateGuidanceImage(progress: RouteProgress)
}
