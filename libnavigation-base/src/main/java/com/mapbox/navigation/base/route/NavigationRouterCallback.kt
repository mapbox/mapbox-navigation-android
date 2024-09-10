package com.mapbox.navigation.base.route

import com.mapbox.api.directions.v5.models.RouteOptions

/**
 * Interface definition for a callback associated with routes request.
 */
interface NavigationRouterCallback {

    /**
     * Called whenever a new set of routes is available.
     *
     * @param routes the most optimal route has index 0. If requested, alternative routes are available on higher indices.
     * At least one route is always available in a successful response.
     * @param routerOrigin route origin
     */
    fun onRoutesReady(routes: List<NavigationRoute>, @RouterOrigin routerOrigin: String)

    /**
     * Called whenever router fails.
     *
     * @param reasons a list of reasons for a failure.
     * The list may contain more than one element if a fallback router was used and failed as well.
     * The elements are ordered by attempt - the first attempt and its failure reason will be at the first index in the list.
     * @param routeOptions the original request options
     */
    fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions)

    /**
     * Called whenever a route request is canceled.
     *
     * @param routeOptions the original request options
     */
    fun onCanceled(routeOptions: RouteOptions, @RouterOrigin routerOrigin: String)
}
