package com.mapbox.navigation.core.trip.session

import com.mapbox.navigation.base.trip.model.alert.RouteAlert
import com.mapbox.navigation.core.MapboxNavigation

/**
 * Observer that gets notified when route changes and new route alerts are available.
 *
 * @see MapboxNavigation.registerRouteAlertsObserver
 */
interface RouteAlertsObserver {

    /**
     * Invoked when the route has changed and new route alerts are available,
     * or when the route is cleared.
     *
     * @param routeAlerts route alerts for the current route, or empty list if the route is cleared.
     */
    fun onNewRouteAlerts(routeAlerts: List<RouteAlert>)
}
