package com.mapbox.navigation.core.internal.router

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.internal.RouteRefreshRequestData
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback

/**
 * API to fetch, refresh and cancel routes request.
 *
 * Provides ability for refreshing routes partially
 * using state snapshot stored in [RouteRefreshRequestData].
 */
internal interface Router {

    /**
     * Fetch routes based on [RouteOptions].
     *
     * @param routeOptions RouteOptions
     * @param signature information about what triggered the route request
     * @param callback Callback that gets notified with the results of the request
     *
     * @return request ID, can be used to cancel the request with [cancelRouteRequest]
     */
    fun getRoute(
        routeOptions: RouteOptions,
        signature: GetRouteSignature,
        callback: NavigationRouterCallback,
    ): Long

    /**
     * Refresh the traffic annotations for a given underlying [DirectionsRoute]
     *
     * @param route [NavigationRoute] the route to refresh
     * @param routeRefreshRequestData Object containing information needed for refresh request
     * @param callback Callback that gets notified with the results of the request
     *
     * @return request ID, can be used to cancel the request with [cancelRouteRefreshRequest]
     */
    fun getRouteRefresh(
        route: NavigationRoute,
        routeRefreshRequestData: RouteRefreshRequestData,
        callback: NavigationRouterRefreshCallback,
    ): Long

    /**
     * Cancels a specific route request.
     */
    fun cancelRouteRequest(requestId: Long)

    /**
     * Interrupts all in-progress requests.
     */
    fun cancelAll()

    /**
     * Cancels a specific route refresh request.
     */
    fun cancelRouteRefreshRequest(requestId: Long)

    /**
     * Release used resources.
     */
    fun shutdown()
}
