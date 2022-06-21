package com.mapbox.navigation.core.testutil

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver

@ExperimentalPreviewMapboxNavigationAPI
internal open class TestMapboxNavigationObserver : MapboxNavigationObserver {
    var attachedTo: MapboxNavigation? = null

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        attachedTo = mapboxNavigation
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        attachedTo = null
    }
}
