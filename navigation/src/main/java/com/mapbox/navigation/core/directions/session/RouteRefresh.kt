package com.mapbox.navigation.core.directions.session

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.internal.RouteRefreshRequestData
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.internal.router.NavigationRouterRefreshCallback

internal interface RouteRefresh {
    /**
     * Refresh the traffic annotations for a given [DirectionsRoute]
     *
     * @param route DirectionsRoute the direction route to refresh
     * @param routeRefreshRequestData Object containing information needed for refresh request
     * @param callback Callback that gets notified with the results of the request
     */
    fun requestRouteRefresh(
        route: NavigationRoute,
        routeRefreshRequestData: RouteRefreshRequestData,
        callback: NavigationRouterRefreshCallback,
    ): Long

    /**
     * Cancels [requestRouteRefresh].
     */
    fun cancelRouteRefreshRequest(requestId: Long)
}
