package com.mapbox.services.android.navigation.v5.navigation

import com.mapbox.api.directions.v5.models.DirectionsRoute

/**
 * Callback used for finding offline routes.
 */
interface OnOfflineRouteFoundCallback {

    /**
     * Called when an offline route is found.
     *
     * @param route offline route
     */
    fun onRouteFound(route: DirectionsRoute)

    /**
     * Called when there was an error fetching the offline route.
     *
     * @param error with message explanation
     */
    fun onError(error: OfflineError)
}
