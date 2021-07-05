package com.mapbox.navigation.base.route

import com.mapbox.api.directions.v5.models.DirectionsRoute

/**
 * Callback for refresh route
 */
// todo move to core and make internal
interface RouteRefreshCallback {
    /**
     * Called when the [DirectionsRoute] has been refreshed
     * @param directionsRoute DirectionsRoute
     */
    fun onRefresh(directionsRoute: DirectionsRoute)

    /**
     * Called when an error has occurred while fetching the route
     * @param error RouteRefreshError
     */
    fun onError(error: RouteRefreshError)
}

/**
 * Route refresh Error
 *
 * @param message described message
 * @param throwable Throwable?
 */
data class RouteRefreshError(
    val message: String? = null,
    val throwable: Throwable? = null
)
