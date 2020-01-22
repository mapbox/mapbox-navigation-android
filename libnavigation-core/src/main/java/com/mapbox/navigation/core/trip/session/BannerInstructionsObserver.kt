package com.mapbox.navigation.core.trip.session

import com.mapbox.api.directions.v5.models.BannerInstructions

interface BannerInstructionsObserver {
    fun onNewBannerInstructions(bannerInstructions: BannerInstructions)
}
