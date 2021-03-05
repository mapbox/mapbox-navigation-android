package com.mapbox.navigation.ui.utils.internal.extensions

import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.BannerView
import com.mapbox.navigation.ui.utils.internal.ifNonNull

/**
 * Given [BannerInstructions] returns [BannerView] if present; null otherwise
 * @return BannerView if present or null
 */
fun BannerInstructions.getBannerView(): BannerView? =
    ifNonNull(view()) {
        it
    }

/**
 * Given [BannerInstructions] returns list of [BannerComponents] if present; null otherwise
 * @return MutableList<BannerComponents>? if present or null
 */
fun BannerInstructions?.getBannerComponents(): MutableList<BannerComponents>? =
    ifNonNull(this?.getBannerView()) { bannerView ->
        ifNonNull(bannerView.components()) {
            it
        }
    }
