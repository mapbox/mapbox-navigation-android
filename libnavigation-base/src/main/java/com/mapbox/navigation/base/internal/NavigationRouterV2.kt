package com.mapbox.navigation.base.internal

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.CurrentIndices
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouter
import com.mapbox.navigation.base.route.NavigationRouterRefreshCallback

/**
 * Extends [NavigationRouter] to also provide ability for refreshing routes partially
 * using indices stored in [CurrentIndices].
 */
interface NavigationRouterV2 : NavigationRouter {

    @Deprecated(
        "Use getRouteRefresh(" +
            "NavigationRoute, " +
            "CurrentIndices, " +
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
     * @param indicesSnapshot Object containing information about consistent current indices
     * @param callback Callback that gets notified with the results of the request
     *
     * @return request ID, can be used to cancel the request with [cancelRouteRefreshRequest]
     */
    fun getRouteRefresh(
        route: NavigationRoute,
        indicesSnapshot: CurrentIndices,
        callback: NavigationRouterRefreshCallback
    ): Long
}
