package com.mapbox.services.android.navigation.v5.route

import android.location.Location
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress

abstract class FasterRoute {

    /**
     * This method determine if a new [DirectionsResponse] should
     * be retrieved by [RouteFetcher].
     *
     * It will also be called every time
     * the <tt>NavigationEngine</tt> gets a valid [Location] update.
     *
     * The most recent snapped location and route progress are provided.  Both can be used to
     * determine if a new route should be fetched or not.
     *
     * @param location current snapped location
     * @param routeProgress current route progress
     * @return true if should check, false if not
     */
    abstract fun shouldCheckFasterRoute(location: Location, routeProgress: RouteProgress): Boolean

    /**
     * This method will be used to determine if the route retrieved is
     * faster than the one that's currently being navigated.
     *
     * @param response provided by [RouteFetcher]
     * @param routeProgress current route progress
     * @return true if the new route is considered faster, false if not
     */
    abstract fun isFasterRoute(response: DirectionsResponse, routeProgress: RouteProgress): Boolean
}
