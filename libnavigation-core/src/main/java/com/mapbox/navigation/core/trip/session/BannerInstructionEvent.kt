package com.mapbox.navigation.core.trip.session

import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.utils.extensions.ifNonNull

class BannerInstructionEvent {

    var bannerInstructions: BannerInstructions? = null
        private set

    fun isOccurring(routeProgress: RouteProgress): Boolean = updateCurrentBanner(routeProgress)

    private fun updateCurrentBanner(routeProgress: RouteProgress): Boolean =
        ifNonNull(routeProgress.bannerInstructions()) {
            bannerInstructions = it
            true
        } ?: false
}
