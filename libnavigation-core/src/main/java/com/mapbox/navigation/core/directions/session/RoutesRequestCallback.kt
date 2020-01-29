package com.mapbox.navigation.core.directions.session

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.route.Router

/**
 * Interface definition for a callback associated with a routes request.
 */
interface RoutesRequestCallback {

    /**
     * Invoked whenever a new set of routes is available.
     *
     * The return type is a list that's going to be assigned to and maintained by the [DirectionsSession].
     * The route at index 0, if exist, will be treated as the primary route for 'Active Guidance' and location map-matching.
     *
     * If an empty list is returned, this request will effectively be ignored by the [DirectionsSession].
     *
     * @param routes list of routes returned by a [Router]
     * @return a list of routes that will be assigned to the session where route at index 0 is the primary one
     */
    fun onRoutesReady(routes: List<DirectionsRoute>): List<DirectionsRoute>

    /**
     * Called whenever this [Router] request fails.
     *
     * @param throwable the exception
     * @param routeOptions the original request options
     */
    // return a boolean here, or a long that will indicate when should we retry the request? or leave that to the user?
    fun onRoutesRequestFailure(throwable: Throwable, routeOptions: RouteOptions)

    /**
     * Called whenever this [Router] request is canceled.
     *
     * @param routeOptions the original request options
     */
    fun onRoutesRequestCanceled(routeOptions: RouteOptions)
}
