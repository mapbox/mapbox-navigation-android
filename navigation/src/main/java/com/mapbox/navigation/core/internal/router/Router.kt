package com.mapbox.navigation.core.internal.router

import androidx.annotation.MainThread
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.internal.RouteRefreshRequestData
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.core.mapmatching.MapMatchingAPICallback
import com.mapbox.navigation.core.mapmatching.MapMatchingOptions

/**
 * API to fetch, refresh and cancel routes request.
 *
 * Provides ability for refreshing routes partially
 * using state snapshot stored in [RouteRefreshRequestData].
 */
@MainThread // Router is not thread safe: in theory it doesn't have to be main thread
internal interface Router {

    /**
     * Fetch routes based on [RouteOptions].
     *
     * @param routeOptions RouteOptions
     * @param signature information about what triggered the route request
     * @param callback Callback that gets notified with the results of the request
     *
     * @return request ID, can be used to cancel the request with [cancelRouteRequest]
     */
    fun getRoute(
        routeOptions: RouteOptions,
        signature: GetRouteSignature,
        callback: NavigationRouterCallback,
    ): Long

    /**
     * Fetch map-matched routes based on [MapMatchingOptions] via the native Map Matching API.
     * The flow follows the same rules as [getRoute] for regular direction routes.
     *
     * @param mapMatchingOptions map matching options (same as for [MapboxNavigation.requestMapMatching])
     * @param signature information about what triggered the route request
     * @param callback Callback that gets notified with the results of the request
     * @return request ID, can be used to cancel the request with [cancelRouteRequest]
     */
    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    fun getRouteMapMatched(
        mapMatchingOptions: MapMatchingOptions,
        signature: GetRouteSignature,
        callback: MapMatchingAPICallback,
    ): Long

    /**
     * Refresh the traffic annotations for a given underlying [DirectionsRoute]
     *
     * @param route [NavigationRoute] the route to refresh
     * @param routeRefreshRequestData Object containing information needed for refresh request
     * @param callback Callback that gets notified with the results of the request
     *
     * @return request ID, can be used to cancel the request with [cancelRouteRefreshRequest]
     */
    fun getRouteRefresh(
        route: NavigationRoute,
        routeRefreshRequestData: RouteRefreshRequestData,
        callback: NavigationRouterRefreshCallback,
    ): Long

    /**
     * Cancels a specific route request.
     */
    fun cancelRouteRequest(requestId: Long)

    /**
     * Cancels a specific map-matched route request.
     */
    fun cancelMapMatchedRouteRequest(requestId: Long)

    /**
     * Interrupts all in-progress requests.
     */
    fun cancelAll()

    /**
     * Cancels a specific route refresh request.
     */
    fun cancelRouteRefreshRequest(requestId: Long)

    /**
     * Release used resources.
     */
    fun shutdown()
}
