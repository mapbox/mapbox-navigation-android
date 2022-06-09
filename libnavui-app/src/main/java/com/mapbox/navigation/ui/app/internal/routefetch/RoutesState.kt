package com.mapbox.navigation.ui.app.internal.routefetch

import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin

/**
 * Defines the state for route between origin and destination.
 */
sealed class RoutesState {
    /**
     * Represents no route available.
     */
    object Empty : RoutesState()

    /**
     * Represents the state when the route is being fetched.
     * @param requestId of the route requested
     */
    data class Fetching internal constructor(val requestId: Long) : RoutesState()

    /**
     * Represents the state when route fetching is complete and the route is ready.
     * @param routes fetched as a result of network request
     */
    data class Ready internal constructor(val routes: List<NavigationRoute>) : RoutesState()

    /**
     * Represents the state when route fetching is canceled.
     * @param routeOptions used to fetch the route
     * @param routerOrigin origin of the route request
     */
    data class Canceled internal constructor(
        val routeOptions: RouteOptions,
        val routerOrigin: RouterOrigin
    ) : RoutesState()

    /**
     * Represents the state when route request is failed.
     * @param reasons for why the request failed
     * @param routeOptions used to fetch the route
     */
    data class Failed internal constructor(
        val reasons: List<RouterFailure>,
        val routeOptions: RouteOptions
    ) : RoutesState()
}
