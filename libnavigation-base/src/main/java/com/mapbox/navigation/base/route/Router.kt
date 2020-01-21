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
     * @param callback Callback that should be handled
     */
    fun getRoute(
        routeOptions: RouteOptions,
        callback: Callback
    )

    /**
     * Interrupt route-fetching request
     */
    fun cancel()

    /**
     * Callback for Router fetching
     */
    interface Callback {

        /**
         * List of [DirectionsRoute]
         *
         * @param routes List<DirectionsRoute> the most relevant has index 0, second is 1 and etc.
         * Has at least one Route
         */
        fun onResponse(routes: List<DirectionsRoute>)

        /**
         * @param throwable Throwable safety error.
         * Called on I/O either when network error occurred or no one Route has been found.
         */
        fun onFailure(throwable: Throwable)
    }
}
