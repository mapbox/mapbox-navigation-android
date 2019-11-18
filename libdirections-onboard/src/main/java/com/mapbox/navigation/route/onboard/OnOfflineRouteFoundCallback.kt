package com.mapbox.navigation.route.onboard

import com.mapbox.navigation.base.route.model.Route
import com.mapbox.navigation.route.onboard.model.OfflineError

/**
 * Callback used for finding offline routes.
 */
interface OnOfflineRouteFoundCallback {

    /**
     * Called when an offline routes are found.
     *
     * @param routes offline routes
     */
    fun onRouteFound(routes: List<Route>)

    /**
     * Called when there was an error fetching the offline route.
     *
     * @param error with message explanation
     */
    fun onError(error: OfflineError)
}
