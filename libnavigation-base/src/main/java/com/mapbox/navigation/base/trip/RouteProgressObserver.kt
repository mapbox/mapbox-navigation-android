package com.mapbox.navigation.base.trip

import com.mapbox.navigation.base.trip.model.RouteProgress

/**
 * Callback that provides state, progress, and other information regarding the current turn-by-turn routing
 *
 * @see [RouteProgress]
 */
interface RouteProgressObserver {
    /**
     * Called every time RouteProgress changes
     */
    fun onRouteProgressChanged(routeProgress: RouteProgress)
}
