package com.mapbox.navigation.core.reroute

import androidx.annotation.UiThread
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.trip.session.OffRouteObserver

/**
 * Reroute controller allows changing the reroute logic externally. Use [MapboxNavigation.rerouteController]
 * to replace it.
 */
@UiThread
interface NavigationRerouteController : RerouteController {

    /**
     * Invoked whenever re-route is needed. For instance when a driver is off-route. Called just after
     * an off-route event.
     *
     * @see [OffRouteObserver]
     */
    fun reroute(callback: RoutesCallback)

    /**
     * Route Callback is useful to set new route(s) on reroute event. Doing the same as
     * [MapboxNavigation.setRoutes].
     */
    @UiThread
    fun interface RoutesCallback {
        /**
         * Called whenever new route(s) has been fetched.
         * @see [MapboxNavigation.setRoutes]
         */
        fun onNewRoutes(routes: List<NavigationRoute>, routerOrigin: RouterOrigin)
    }
}
