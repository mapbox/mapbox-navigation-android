package com.mapbox.navigation.core.stops

import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.core.MapboxNavigation

/**
 * This gives many observers the ability to monitor the progress of arrival.
 * To control the behavior of arrival, see [ArrivalController]
 */
interface ArrivalObserver {

    /**
     * Once [MapboxNavigation.navigateNextRouteLeg] has been called and returns true,
     * this observer will be notified of a stop arrival.
     */
    fun onStopArrival(routeLegProgress: RouteLegProgress) {}

    /**
     * Once the [RouteProgress.currentState] has reached [RouteProgressState.ROUTE_ARRIVED]
     * for the last stop, this will be called once.
     */
    fun onRouteArrival(routeProgress: RouteProgress) {}
}
