package com.mapbox.navigation.core.directions.session

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouter
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.NavigationRouterRefreshCallback

internal class MapboxDirectionsSession(
    private val router: NavigationRouter
) {

    /**
     * Interrupts a route-fetching request if one is in progress.
     */
    fun cancelAll() {
        router.cancelAll()
    }

    /**
     * Refresh the traffic annotations for a given [DirectionsRoute]
     *
     * @param route DirectionsRoute the direction route to refresh
     * @param legIndex Int the index of the current leg in the route
     * @param callback Callback that gets notified with the results of the request
     */
    fun requestRouteRefresh(
        route: NavigationRoute,
        legIndex: Int,
        callback: NavigationRouterRefreshCallback
    ): Long {
        return router.getRouteRefresh(route, legIndex, callback)
    }

    /**
     * Cancels [requestRouteRefresh].
     */
    fun cancelRouteRefreshRequest(requestId: Long) {
        router.cancelRouteRefreshRequest(requestId)
    }

    /**
     * Fetch route based on [RouteOptions]
     *
     * @param routeOptions RouteOptions
     * @param routerCallback Callback that gets notified with the results of the request(optional),
     * see [registerRoutesObserver]
     *
     * @return requestID, see [cancelRouteRequest]
     */
    fun requestRoutes(
        routeOptions: RouteOptions,
        routerCallback: NavigationRouterCallback
    ): Long {
        return router.getRoute(routeOptions, routerCallback)
    }

    fun cancelRouteRequest(requestId: Long) {
        router.cancelRouteRequest(requestId)
    }

    /**
     * Interrupt route-fetcher request
     */
    fun shutdown() {
        router.shutdown()
    }
}
