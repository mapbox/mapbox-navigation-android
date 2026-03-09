package com.mapbox.navigation.testing.utils.idling

import androidx.test.espresso.IdlingResource
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.testing.ui.idling.NavigationIdlingResource

/**
 * Becomes idle when [RouteProgressObserver.onRouteProgressChanged] gets invoked
 * and `currentRouteProgressState == awaitedProgressState`.
 *
 * This is detected automatically when `mapboxNavigation` is provided.
 * Otherwise, it should be invoked manually with
 * [RouteProgressStateIdlingResource.onRouteProgressChanged].
 */
class RouteProgressStateIdlingResource(
    private val mapboxNavigation: MapboxNavigation? = null,
    private val awaitedProgressState: RouteProgressState
) : NavigationIdlingResource(), RouteProgressObserver {

    private var currentRouteProgressState: RouteProgressState? = null

    private var callback: IdlingResource.ResourceCallback? = null

    override fun getName() = "RouteProgressStateIdlingResource"

    override fun isIdleNow() = currentRouteProgressState == awaitedProgressState

    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
        this.callback = callback
        mapboxNavigation?.registerRouteProgressObserver(this)
    }

    override fun onRouteProgressChanged(routeProgress: RouteProgress) {
        currentRouteProgressState = routeProgress.currentState
        if (isIdleNow) {
            mapboxNavigation?.unregisterRouteProgressObserver(this)
            callback?.onTransitionToIdle()
        }
    }
}
