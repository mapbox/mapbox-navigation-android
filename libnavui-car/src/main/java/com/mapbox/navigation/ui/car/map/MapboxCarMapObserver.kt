package com.mapbox.navigation.ui.car.map

import android.graphics.Rect
import com.mapbox.maps.EdgeInsets
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI

/**
 * Many downstream services will not work until the surface has been created.
 * This class allows us to extend the map surface without changing the internal implementation.
 */
@ExperimentalMapboxNavigationAPI
interface MapboxCarMapObserver {

    /**
     * Called when a [MapboxCarMapSurface] has been loaded.
     * Safe to assume there will only be a single surface at a time.
     */
    fun loaded(mapboxCarMapSurface: MapboxCarMapSurface) {
        // No op by default
    }

    /**
     * Called when a car library updates the visible regions for the surface.
     * Safe to assume this will be called after [loaded].
     */
    fun visibleAreaChanged(visibleArea: Rect, edgeInsets: EdgeInsets) {
        // No op by default
    }

    /**
     * Called when a [MapboxCarMapSurface] is detached.
     * This is null when the map surface did not complete finish loading.
     */
    fun detached(mapboxCarMapSurface: MapboxCarMapSurface) {
        // No op by default
    }
}
