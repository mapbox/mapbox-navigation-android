package com.mapbox.services.android.navigation.v5.route

import android.location.Location

import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress

/**
 * This class can be subclassed to provide custom logic for checking / determining
 * new / faster routes while navigating.
 *
 *
 * To provide your implementation,
 * use [com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation.setFasterRouteEngine].
 *
 *
 * [FasterRoute.shouldCheckFasterRoute] determines how quickly a
 * new route will be fetched by [RouteFetcher].
 *
 *
 * [FasterRoute.isFasterRoute] determines if the new route
 * retrieved by [RouteFetcher] is actually faster than the current route.
 *
 */
abstract class FasterRoute {

    /**
     * This method determine if a new [DirectionsResponse] should
     * be retrieved by [RouteFetcher].
     *
     *
     * It will also be called every time
     * the <tt>NavigationEngine</tt> gets a valid [Location] update.
     *
     *
     * The most recent snapped location and route progress are provided.  Both can be used to
     * determine if a new route should be fetched or not.
     *
     * @param location      current snapped location
     * @param routeProgress current route progress
     * @return true if should check, false if not
     * @since 0.9.0
     */
    abstract fun shouldCheckFasterRoute(location: Location, routeProgress: RouteProgress): Boolean

    /**
     * This method will be used to determine if the route retrieved is
     * faster than the one that's currently being navigated.
     *
     * @param response      provided by [RouteFetcher]
     * @param routeProgress current route progress
     * @return true if the new route is considered faster, false if not
     */
    abstract fun isFasterRoute(response: DirectionsResponse, routeProgress: RouteProgress): Boolean
}
