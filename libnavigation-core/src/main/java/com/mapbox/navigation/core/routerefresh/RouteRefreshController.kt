package com.mapbox.navigation.core.routerefresh

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.directions.session.RoutesObserver

/**
 * This class lets you manage route refreshes.
 */
@ExperimentalPreviewMapboxNavigationAPI
interface RouteRefreshController {

    /**
     * Immediately refresh current navigation routes.
     * Listen for refreshed routes using [RoutesObserver].
     *
     * The on-demand refresh request is not guaranteed to succeed and call the [RoutesObserver],
     * [requestImmediateRouteRefresh] invocations cannot be coupled with
     * [RoutesObserver.onRoutesChanged] callbacks for state management.
     * You can use [registerRouteRefreshStateObserver] to monitor refresh statuses independently.
     */
    fun requestImmediateRouteRefresh()

    /**
     * Register a [RouteRefreshStatesObserver] to be notified of Route refresh state changes.
     *
     * @param routeRefreshStatesObserver RouteRefreshStatesObserver
     */
    fun registerRouteRefreshStateObserver(
        routeRefreshStatesObserver: RouteRefreshStatesObserver
    )

    /**
     * Unregisters a [RouteRefreshStatesObserver].
     */
    fun unregisterRouteRefreshStateObserver(
        routeRefreshStatesObserver: RouteRefreshStatesObserver
    )
}
