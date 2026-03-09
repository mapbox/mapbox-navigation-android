package com.mapbox.navigation.testing.utils.idling

import androidx.test.espresso.IdlingResource
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.navigation.testing.ui.idling.NavigationIdlingResource

/**
 * Becomes idle when [Style] is loaded.
 */
class MapStyleInitIdlingResource(
    private val mapView: MapView
) : NavigationIdlingResource() {

    private var initialized = false

    override fun getName() = this::class.simpleName

    override fun isIdleNow() = initialized

    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
        mapView.getMapboxMap().getStyle(object : Style.OnStyleLoaded {
            override fun onStyleLoaded(style: Style) {
                initialized = true
                callback?.onTransitionToIdle()
            }
        })
    }
}
