package com.mapbox.navigation.ui.puck

import com.mapbox.navigation.base.trip.model.RouteProgressState

interface PuckDrawableSupplier {
    /**
     * Based on the @param routeProgressState return a drawable to represent the puck
     * used on the map while navigating.
     */
    fun getPuckDrawable(routeProgressState: RouteProgressState): Int
}
