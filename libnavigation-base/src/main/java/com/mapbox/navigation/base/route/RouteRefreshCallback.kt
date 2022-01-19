package com.mapbox.navigation.base.route

import com.mapbox.api.directions.v5.models.DirectionsRoute

/**
 * Interface definition for a callback associated with routes refresh.
 */
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
