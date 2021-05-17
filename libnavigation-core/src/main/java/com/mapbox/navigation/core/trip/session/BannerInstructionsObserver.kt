package com.mapbox.navigation.core.trip.session

import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.navigation.base.trip.model.RouteProgress

/**
 * Banner instruction that is helpful for turn-by-turn routing instructions. The [BannerInstructions]
 * information is updated on route progress.
 * @see [RouteProgress]
 */
fun interface BannerInstructionsObserver {

    /**
     * Called whenever new banner instruction available
     * @see [RouteProgress]
     */
    fun onNewBannerInstructions(bannerInstructions: BannerInstructions)
}
