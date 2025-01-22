package com.mapbox.navigation.core.trip.session

import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.navigation.utils.internal.ifNonNull

internal class BannerInstructionEvent private constructor() {

    var latestInstructionWrapper: LatestInstructionWrapper? = null
        private set

    val latestInstructionIndex: Int?
        get() = latestInstructionWrapper?.latestInstructionIndex

    val latestBannerInstructions: BannerInstructions?
        get() = latestInstructionWrapper?.latestBannerInstructions

    var bannerInstructions: BannerInstructions? = null
        private set

    companion object {
        operator fun invoke(): BannerInstructionEvent = BannerInstructionEvent()
    }

    fun isOccurring(bannerInstructions: BannerInstructions?, instructionIndex: Int?): Boolean {
        return updateCurrentBanner(bannerInstructions, instructionIndex)
    }

    fun invalidateLatestBannerInstructions(latestInstructionWrapper: LatestInstructionWrapper?) {
        if (latestInstructionWrapper == this.latestInstructionWrapper) {
            this.latestInstructionWrapper = null
        }
    }

    private fun updateCurrentBanner(banner: BannerInstructions?, instructionIndex: Int?): Boolean {
        bannerInstructions = banner
        if (bannerInstructions != null && bannerInstructions!! != latestBannerInstructions) {
            latestInstructionWrapper =
                LatestInstructionWrapper.createOrNull(instructionIndex, bannerInstructions)
            return true
        }
        return false
    }

    data class LatestInstructionWrapper(
        val latestInstructionIndex: Int,
        val latestBannerInstructions: BannerInstructions,
    ) {
        companion object {
            fun createOrNull(
                latestInstructionIndex: Int?,
                latestBannerInstructions: BannerInstructions?,
            ): LatestInstructionWrapper? =
                ifNonNull(latestInstructionIndex, latestBannerInstructions) { idx, instruction ->
                    LatestInstructionWrapper(idx, instruction)
                }
        }
    }
}
