package com.mapbox.navigation.ui.base.domain

import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.BannerView
import com.mapbox.navigation.ui.utils.internal.ifNonNull

interface BannerInstructionsApi {

    fun getBannerView(bannerInstructions: BannerInstructions): BannerView? {
        return ifNonNull(bannerInstructions.view()) {
            it
        }
    }

    fun getBannerComponents(
        bannerInstructions: BannerInstructions
    ): MutableList<BannerComponents>? {
        return ifNonNull(getBannerView(bannerInstructions)) { bannerView ->
            ifNonNull(bannerView.components()) {
                it
            }
        }
    }
}
