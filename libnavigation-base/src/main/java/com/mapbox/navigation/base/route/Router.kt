package com.mapbox.navigation.base.route

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions

/**
 * Router provides API to fetch route and cancel route-fetching request.
 */
interface Router {

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
        callback: Callback
    ): Long

    /**
     * Cancels a specific route request.
     *
     * @see [getRoute]
     */
    fun cancelRouteRequest(requestId: Long)

    /**
     * Interrupts all in-progress requests.
     */
    fun cancelAll()

    /**
     * Refresh the traffic annotations for a given [DirectionsRoute]
     *
     * @param route DirectionsRoute the direction route to refresh
     * @param legIndex Int the index of the current leg in the route
     * @param callback Callback that gets notified with the results of the request
     *
     * @return request ID, can be used to cancel the request with [cancelRouteRefreshRequest]
     */
    fun getRouteRefresh(
        route: DirectionsRoute,
        legIndex: Int,
        callback: RouteRefreshCallback
    ): Long

    /**
     * Cancels a specific route refresh request.
     *
     * @see [getRouteRefresh]
     */
    fun cancelRouteRefreshRequest(requestId: Long)

    /**
     * Release used resources.
     */
    fun shutdown()

    /**
     * Callback for Router fetching
     */
    interface Callback {

        /**
         * [RouteWrapper]: non-empty list of [DirectionsRoute] and [RouteVariants.RouterOrigin]
         *
         * @param routeWrapper has List<DirectionsRoute> the most relevant has index 0.
         * If requested, alternative routes are available on higher indices.
         * Has at least one Route
         */
        fun onResponse(routeWrapper: RouteWrapper)

        /**
         * @param throwable Throwable safety error.
         * Called when I/O, server-side, network error has been occurred or no one Route has been found.
         */
        fun onFailure(throwable: Throwable)

        /**
         * Called whenever a route request is canceled.
         */
        fun onCanceled()
    }
}
