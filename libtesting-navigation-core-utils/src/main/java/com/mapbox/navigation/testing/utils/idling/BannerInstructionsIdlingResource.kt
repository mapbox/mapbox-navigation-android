package com.mapbox.navigation.testing.utils.idling

import androidx.test.espresso.IdlingResource
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.trip.session.BannerInstructionsObserver
import com.mapbox.navigation.testing.ui.idling.NavigationIdlingResource

/**
 * Becomes idle when [BannerInstructionsObserver.onNewBannerInstructions] gets invoked
 * and `bannerInstructions == expectedBannerInstructions`.
 *
 * This is detected automatically when `mapboxNavigation` is provided.
 * Otherwise, it should be invoked manually with
 * [BannerInstructionsIdlingResource.onNewBannerInstructions].
 */
class BannerInstructionsIdlingResource(
    private val mapboxNavigation: MapboxNavigation? = null,
    private val expectedBannerInstructions: BannerInstructions
) : NavigationIdlingResource(), BannerInstructionsObserver {

    private var idle = false

    private var callback: IdlingResource.ResourceCallback? = null

    override fun getName() = "BannerInstructionsIdlingResource"

    override fun isIdleNow() = idle

    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
        this.callback = callback
        mapboxNavigation?.registerBannerInstructionsObserver(this)
    }

    override fun onNewBannerInstructions(bannerInstructions: BannerInstructions) {
        if (bannerInstructions == expectedBannerInstructions) {
            mapboxNavigation?.unregisterBannerInstructionsObserver(this)
            idle = true
            callback?.onTransitionToIdle()
        }
    }
}
