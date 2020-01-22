package com.mapbox.navigation.trip.session

import com.mapbox.api.directions.v5.models.BannerInstructions

interface BannerInstructionsObserver {
    fun onNewBannerInstructions(bannerInstructions: BannerInstructions)
}
