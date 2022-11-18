package com.mapbox.navigation.core.routealternatives

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.MapboxNavigation

/**
 * Interface that allows for observing incoming online routes.
 * See [MapboxNavigation.registerOffboardRoutesObserver].
 */
fun interface OffboardRoutesObserver {

    /**
     * This method is invoked when current primary route has `Onboard` origin,
     * and some incoming route is a sub-route of the primary route and it has `Online` origin.
     * If you want to switch to online route on the fly, implement your observer the following way:
     * ```
     * val observer = OffboardRoutesObserver { newOnlineRoutes ->
     *     mapboxNavigation.setNavigationRoutes(newOnlineRoutes)
     * }
     * ```
     * then register it:
     * ```
     * mapboxNavigation.registerOffboardRoutesObserver(observer)
     * ```
     *
     * @param routes new incoming routes, that have `Online` origin
     *  and are sub-routes of current routes
     */
    fun onOffboardRoutesAvailable(routes: List<NavigationRoute>)
}
