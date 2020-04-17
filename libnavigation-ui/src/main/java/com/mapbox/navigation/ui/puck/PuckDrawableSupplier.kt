package com.mapbox.navigation.ui.puck

import com.mapbox.navigation.base.trip.model.RouteProgressState

/**
 * Allows custom implementation of different drawables for puck
 */
interface PuckDrawableSupplier {
    /**
     * Based on the @param routeProgressState return a drawable to represent the puck
     * used on the map while navigating.
     *
     * @param routeProgressState various progress state that can occur while navigating
     * @return drawable associated to the [com.mapbox.navigation.base.trip.model.RouteProgressState]
     */
    fun getPuckDrawable(routeProgressState: RouteProgressState): Int
}
