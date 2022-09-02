package com.mapbox.navigation.core.trip.session

import androidx.annotation.UiThread
import com.mapbox.navigation.base.trip.model.RouteProgress

/**
 * Callback that provides state, progress, and other information regarding the current turn-by-turn routing
 *
 * @see [RouteProgress]
 */
@UiThread
fun interface RouteProgressObserver {
    /**
     * Invoked every time the [RouteProgress] is updated
     * @param routeProgress [RouteProgress]
     */
    fun onRouteProgressChanged(routeProgress: RouteProgress)
}
