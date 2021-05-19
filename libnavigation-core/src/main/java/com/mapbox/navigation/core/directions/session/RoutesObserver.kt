package com.mapbox.navigation.core.directions.session

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.core.MapboxNavigation

/**
 * Interface definition for an observer that gets notified whenever a list of maintained routes changes.
 */
fun interface RoutesObserver {

    /**
     * Invoked whenever a list of routes changes.
     *
     * The route at index 0, if exist, will be treated as the primary route for 'Active Guidance'.
     *
     * A list of routes can be modified internally and externally at any time with [MapboxNavigation.setRoutes], or during automatic reroutes, faster route and route refresh operations.
     *
     * @param routes list of currently maintained routes
     */
    fun onRoutesChanged(routes: List<DirectionsRoute>)
}
