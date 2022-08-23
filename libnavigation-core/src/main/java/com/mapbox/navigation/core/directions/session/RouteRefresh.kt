package com.mapbox.navigation.core.directions.session

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.internal.CurrentIndices
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterRefreshCallback

internal interface RouteRefresh {
    /**
     * Refresh the traffic annotations for a given [DirectionsRoute]
     *
     * @param route DirectionsRoute the direction route to refresh
     * @param currentIndices Object containing information about consistent current indices
     * @param callback Callback that gets notified with the results of the request
     */
    fun requestRouteRefresh(
        route: NavigationRoute,
        currentIndices: CurrentIndices,
        callback: NavigationRouterRefreshCallback
    ): Long

    /**
     * Cancels [requestRouteRefresh].
     */
    fun cancelRouteRefreshRequest(requestId: Long)
}
