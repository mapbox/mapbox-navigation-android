package com.mapbox.navigation.ui.app.internal.routefetch

import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin

/**
 * Defines the state for route between origin and destination.
 */
sealed class RoutePreviewState {
    /**
     * Represents no route available.
     */
    object Empty : RoutePreviewState()

    /**
     * Represents the state when the route is being fetched.
     * @param requestId of the route requested
     */
    data class Fetching constructor(val requestId: Long) : RoutePreviewState()

    /**
     * Represents the state when route fetching is complete and the route is ready.
     * @param routes fetched as a result of network request
     */
    data class Ready constructor(val routes: List<NavigationRoute>) : RoutePreviewState()

    /**
     * Represents the state when route fetching is canceled.
     * @param routeOptions used to fetch the route
     * @param routerOrigin origin of the route request
     */
    data class Canceled constructor(
        val routeOptions: RouteOptions,
        val routerOrigin: RouterOrigin
    ) : RoutePreviewState()

    /**
     * Represents the state when route request is failed.
     * @param reasons for why the request failed
     * @param routeOptions used to fetch the route
     */
    data class Failed constructor(
        val reasons: List<RouterFailure>,
        val routeOptions: RouteOptions
    ) : RoutePreviewState()
}
