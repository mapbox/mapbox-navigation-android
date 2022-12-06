package com.mapbox.navigation.core.trip.session

import com.mapbox.api.directions.v5.models.BannerInstructions

internal class BannerInstructionEvent {

    var latestInstructionIndex: Int? = null
        private set

    var bannerInstructions: BannerInstructions? = null
        private set

    var latestBannerInstructions: BannerInstructions? = null
        private set

    fun isOccurring(bannerInstructions: BannerInstructions?, instructionIndex: Int?): Boolean {
        return updateCurrentBanner(bannerInstructions, instructionIndex)
    }

    fun invalidateLatestBannerInstructions() {
        latestBannerInstructions = null
        latestInstructionIndex = null
    }

    private fun updateCurrentBanner(banner: BannerInstructions?, instructionIndex: Int?): Boolean {
        bannerInstructions = banner
        if (bannerInstructions != null && bannerInstructions!! != latestBannerInstructions) {
            latestBannerInstructions = bannerInstructions
            latestInstructionIndex = instructionIndex
            return true
        }
        return false
    }
}
