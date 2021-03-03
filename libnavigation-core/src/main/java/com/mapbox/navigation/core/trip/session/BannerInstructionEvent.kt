package com.mapbox.navigation.core.trip.session

import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.utils.internal.ifNonNull

internal class BannerInstructionEvent {

    var bannerInstructions: BannerInstructions? = null
        private set

    var latestBannerInstructions: BannerInstructions? = null
        private set

    fun isOccurring(routeProgress: RouteProgress) = updateCurrentBanner(routeProgress)

    fun invalidateLatestBannerInstructions() {
        latestBannerInstructions = null
    }

    private fun updateCurrentBanner(routeProgress: RouteProgress) {
        bannerInstructions = routeProgress.bannerInstructions
        ifNonNull(bannerInstructions) {
            latestBannerInstructions = it
        }
    }
}
