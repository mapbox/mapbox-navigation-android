package com.mapbox.navigation.ui.puck

import androidx.annotation.DrawableRes
import com.mapbox.navigation.base.trip.model.RouteProgressState

/**
 * Allows custom implementation of different drawables for puck
 */
interface PuckDrawableSupplier {
    /**
     * Based on the @param routeProgressState return a drawable resource int to represent the puck
     * used on the map while navigating.
     *
     * @param routeProgressState various progress state that can occur while navigating
     * @return drawable associated to the [com.mapbox.navigation.base.trip.model.RouteProgressState]
     */
    @DrawableRes
    fun getPuckDrawable(routeProgressState: RouteProgressState): Int
}
