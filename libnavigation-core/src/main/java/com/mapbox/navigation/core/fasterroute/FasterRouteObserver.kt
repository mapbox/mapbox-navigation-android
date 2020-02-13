package com.mapbox.navigation.core.fasterroute

import com.mapbox.api.directions.v5.models.DirectionsRoute

/**
 * Interface definition for an observer that get's notified whenever the SDK finds a faster route to the destination.
 */
interface FasterRouteObserver {

    /**
     * Invoked whenever a faster route is available for user to use.
     *
     * @param fasterRoute reference to route that is faster than the current route.
     */
    fun onFasterRouteAvailable(fasterRoute: DirectionsRoute)
}
