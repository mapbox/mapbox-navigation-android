package com.mapbox.navigation.core.trip.session

import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.navigation.base.trip.model.RouteProgress

internal class BannerInstructionEvent {

    var bannerInstructions: BannerInstructions? = null
        private set

    var latestBannerInstructions: BannerInstructions? = null
        private set

    fun isOccurring(routeProgress: RouteProgress): Boolean = updateCurrentBanner(routeProgress)

    fun invalidateLatestBannerInstructions() {
        latestBannerInstructions = null
    }

    private fun updateCurrentBanner(routeProgress: RouteProgress): Boolean {
        bannerInstructions = routeProgress.bannerInstructions
        if (bannerInstructions != null && bannerInstructions!! != latestBannerInstructions) {
            latestBannerInstructions = bannerInstructions
            return true
        }
        return false
    }
}
