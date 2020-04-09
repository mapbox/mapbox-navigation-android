package com.mapbox.navigation.core.directions.session

import com.mapbox.api.directions.v5.models.DirectionsRoute

/**
 * Interface definition for an observer that get's notified whenever a list of maintained routes changes.
 */
interface RoutesObserver {

    /**
     * Invoked whenever a list of routes changes.
     *
     * The route at index 0, if exist, will be treated as the primary route for 'Active Guidance' and location map-matching.
     *
     * @param routes list of currently maintained routes
     */
    fun onRoutesChanged(routes: List<DirectionsRoute>)
}
