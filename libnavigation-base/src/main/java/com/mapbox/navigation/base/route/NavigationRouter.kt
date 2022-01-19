package com.mapbox.navigation.base.route

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions

/**
 * Extends [Router] to also provide ability for fetching and refreshing [NavigationRoute]s.
 */
interface NavigationRouter : Router {

    /**
     * Fetch routes based on [RouteOptions].
     *
     * @param routeOptions RouteOptions
     * @param callback Callback that gets notified with the results of the request
     *
     * @return request ID, can be used to cancel the request with [cancelRouteRequest]
     */
    fun getRoute(
        routeOptions: RouteOptions,
        callback: NavigationRouterCallback
    ): Long

    /**
     * Refresh the traffic annotations for a given underlying [DirectionsRoute]
     *
     * @param route [NavigationRoute] the route to refresh
     * @param legIndex Int the index of the current leg in the route to refresh
     * @param callback Callback that gets notified with the results of the request
     *
     * @return request ID, can be used to cancel the request with [cancelRouteRefreshRequest]
     */
    fun getRouteRefresh(
        route: NavigationRoute,
        legIndex: Int,
        callback: NavigationRouterRefreshCallback
    ): Long
}
