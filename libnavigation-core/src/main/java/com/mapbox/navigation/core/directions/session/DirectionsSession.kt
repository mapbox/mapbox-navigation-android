package com.mapbox.navigation.core.directions.session

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.route.RouteRefreshCallback
import com.mapbox.navigation.base.route.Router

internal interface DirectionsSession {

    /**
     * Routes that were fetched from [Router] or set manually.
     * On [routes] change notify registered [RoutesObserver]
     *
     * @see [registerRoutesObserver]
     */
    var routes: List<DirectionsRoute>

    /**
     * Provide route options for current [routes]
     */
    fun getRouteOptions(): RouteOptions?

    /**
     * Fetch route based on [RouteOptions]
     *
     * @param routeOptions RouteOptions
     * @param routesRequestCallback Callback that gets notified with the results of the request
     */
    fun requestRoutes(
        routeOptions: RouteOptions,
        routesRequestCallback: RoutesRequestCallback? = null
    )

    /**
     * Requests a route using the provided [Router] implementation.
     * Unlike [DirectionsSession.requestRoutes] it ignores the result and it's up to the
     * consumer to take an action with the route.
     *
     * @param adjustedRouteOptions: RouteOptions with adjusted parameters
     * @param routesRequestCallback Callback that gets notified when request state changes
     */
    fun requestFasterRoute(
        adjustedRouteOptions: RouteOptions,
        routesRequestCallback: RoutesRequestCallback
    )

    /**
     * Refresh the traffic annotations for a given [DirectionsRoute]
     *
     * @param route DirectionsRoute the direction route to refresh
     * @param legIndex Int the index of the current leg in the route
     * @param callback Callback that gets notified with the results of the request
     */
    fun requestRouteRefresh(route: DirectionsRoute, legIndex: Int, callback: RouteRefreshCallback)

    /**
     * Interrupts a route-fetching request if one is in progress.
     */
    fun cancel()

    /**
     * Registers [RoutesObserver]. Updated on each change of [routes]
     */
    fun registerRoutesObserver(routesObserver: RoutesObserver)

    /**
     * Unregisters [RoutesObserver]
     */
    fun unregisterRoutesObserver(routesObserver: RoutesObserver)

    /**
     * Unregisters all [RoutesObserver]
     */
    fun unregisterAllRoutesObservers()

    /**
     * Interrupts the route-fetching request
     */
    fun shutdown()
}
