package com.mapbox.navigation.base.route

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions

/**
 * Router provides API to fetch route and cancel route-fetching request.
 */
interface Router {

    /**
     * Fetch route based on [RouteOptions]
     *
     * @param routeOptions RouteOptions
     * @param callback Callback that gets notified with the results of the request
     */
    fun getRoute(
        routeOptions: RouteOptions,
        callback: Callback
    )

    /**
     * Interrupt route-fetching request, if any is in progress.
     */
    fun cancel()

    fun shutdown()

    /**
     * Callback for Router fetching
     */
    interface Callback {

        /**
         * Non-empty list of [DirectionsRoute]
         *
         * @param routes List<DirectionsRoute> the most relevant has index 0. If requested, alternative routes are available on higher indices.
         * Has at least one Route
         */
        fun onResponse(routes: List<DirectionsRoute>)

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
