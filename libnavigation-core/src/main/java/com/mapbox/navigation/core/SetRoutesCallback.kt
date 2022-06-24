package com.mapbox.navigation.core

import com.mapbox.navigation.base.route.NavigationRoute

/**
 * Interface definition for a callback that gets notified whenever routes
 * passed to [MapboxNavigation.setNavigationRoutes] are set or produce an error and ignored.
 */
interface SetRoutesCallback {

    /**
     * Invoked whenever the routes passed to [MapboxNavigation.setNavigationRoutes]
     * are successfully set.
     *
     * @param routes List of routes that were successfully set.
     */
    fun onRoutesSetResult(routes: List<NavigationRoute>)

    /**
     * Invoked whenever the routes passed to [MapboxNavigation.setNavigationRoutes]
     * produce an error and are ignored.
     *
     * @param routes List of routes that were ignored.
     * @param error Reason why routes were ignored.
     */
    fun onRoutesSetError(routes: List<NavigationRoute>, error: String?)
}
