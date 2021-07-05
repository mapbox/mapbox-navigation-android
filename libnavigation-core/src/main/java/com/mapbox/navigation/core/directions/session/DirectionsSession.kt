package com.mapbox.navigation.core.directions.session

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.route.RouteRefreshCallback
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.base.route.RouterCallback

internal interface DirectionsSession {

    /**
     * Routes that were fetched from [Router] or set manually.
     * On [routes] change notify registered [RoutesObserver]
     *
     * @see [registerRoutesObserver]
     */
    var routes: List<DirectionsRoute>

    /**
     * Provide route options for current primary route.
     */
    fun getPrimaryRouteOptions(): RouteOptions?

    /**
     * Fetch route based on [RouteOptions]
     *
     * @param routeOptions RouteOptions
     * @param routerCallback Callback that gets notified with the results of the request
     *
     * @return requestID, see [cancelRouteRequest]
     */
    fun requestRoutes(
        routeOptions: RouteOptions,
        routerCallback: RouterCallback
    ): Long

    /**
     * Interrupts a route-fetching request if one is in progress.
     */
    fun cancelRouteRequest(requestId: Long)

    /**
     * Refresh the traffic annotations for a given [DirectionsRoute]
     *
     * @param route DirectionsRoute the direction route to refresh
     * @param legIndex Int the index of the current leg in the route
     * @param callback Callback that gets notified with the results of the request
     */
    fun requestRouteRefresh(
        route: DirectionsRoute,
        legIndex: Int,
        callback: RouteRefreshCallback
    ): Long

    /**
     * Cancels [requestRouteRefresh].
     */
    fun cancelRouteRefreshRequest(requestId: Long)

    /**
     * Interrupts all requests if any are in progress.
     */
    fun cancelAll()

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
