package com.mapbox.navigation.core.directions.session

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.core.MapboxNavigation

/**
 * Interface definition for a callback associated with a routes request.
 */
interface RoutesRequestCallback {

    /**
     * Invoked whenever a new set of routes is available.
     *
     * The provided list is the list of routes created by this request and assigned to be managed by the SDK.
     * The route at index 0, if exist, is treated as the primary route for 'Active Guidance'.
     *
     * The list provided by this callback is not guaranteed to still be the one managed by the SDK at the moment of invocation.
     * Use [RoutesObserver] and [MapboxNavigation.registerRoutesObserver] to observe whenever the routes list reference managed by the SDK changes, regardless of a source.
     *
     * Use [MapboxNavigation.setRoutes] to supply a transformed list of routes, or a list from an external source, to be managed by the SDK.
     *
     * @param routes list of routes returned by a [Router]
     */
    fun onRoutesReady(routes: List<DirectionsRoute>)

    /**
     * Called whenever this [Router] request fails.
     *
     * @param throwable the exception
     * @param routeOptions the original request options
     */
    fun onRoutesRequestFailure(throwable: Throwable, routeOptions: RouteOptions)

    /**
     * Called whenever this [Router] request is canceled.
     *
     * @param routeOptions the original request options
     */
    fun onRoutesRequestCanceled(routeOptions: RouteOptions)
}
