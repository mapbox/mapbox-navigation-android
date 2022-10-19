package com.mapbox.navigation.base.internal

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouter
import com.mapbox.navigation.base.route.NavigationRouterRefreshCallback

/**
 * Extends [NavigationRouter] to also provide ability for refreshing routes partially
 * using state snapshot stored in [RouteRefreshRequestData].
 */
interface NavigationRouterV2 : NavigationRouter {

    @Deprecated(
        "Use getRouteRefresh(" +
            "NavigationRoute, " +
            "RouteRefreshRequestData, " +
            "NavigationRouterRefreshCallback" +
            ")"
    )
    override fun getRouteRefresh(
        route: NavigationRoute,
        legIndex: Int,
        callback: NavigationRouterRefreshCallback
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
        callback: NavigationRouterRefreshCallback
    ): Long
}
