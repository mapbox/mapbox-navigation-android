package com.mapbox.navigation.ui.maps.route.line.api

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class DelayedRoutesRenderedCallback(
    private val originalCallback: RoutesRenderedCallback,
) : RoutesRenderedCallback {

    private var result: RoutesRenderedResult? = null
    private var isUnlocked = false

    override fun onRoutesRendered(result: RoutesRenderedResult) {
        if (isUnlocked) {
            originalCallback.onRoutesRendered(result)
        } else {
            this.result = result
        }
    }

    fun unlock() {
        isUnlocked = true
        result?.let { originalCallback.onRoutesRendered(it) }
        result = null
    }
}
