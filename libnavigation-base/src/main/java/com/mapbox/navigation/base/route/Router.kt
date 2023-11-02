package com.mapbox.navigation.base.route

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.internal.route.RetryableThrowable
import java.net.URL

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
        callback: RouterCallback
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
     * @param legIndex Int the index of the current leg in the route to refresh
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
}

/**
 * Describes a reason for a route request failure.
 *
 * @param url original request URL
 * @param routerOrigin router that failed to generate a route
 * @param message message attached to the error code
 * @param code if present, can be either be the HTTP code for offboard requests
 * or an internal error code for onboard requests
 * @param throwable provided if an unexpected exception occurred when creating the request or processing the response
 */
data class RouterFailure @JvmOverloads constructor(
    val url: URL,
    val routerOrigin: RouterOrigin,
    val message: String,
    val code: Int? = null,
    val throwable: Throwable? = null
) {
    /**
     * Indicates if it makes sense to retry for this type of failure.
     * If false, it doesn't make sense to retry route request
     */
    val isRetryable get() = throwable is RetryableThrowable
}
