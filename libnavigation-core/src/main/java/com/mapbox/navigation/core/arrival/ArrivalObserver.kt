package com.mapbox.navigation.core.arrival

import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.core.MapboxNavigation

/**
 * This gives many observers the ability to monitor the progress of arrival.
 * To control the behavior of arrival, see [ArrivalController].
 */
interface ArrivalObserver {

    /**
     * Triggered when the [RouteProgress.currentState] is equal to [RouteProgressState.COMPLETE],
     * once per route leg, and when there are more route legs to navigate. If we're on the last leg of the route [onFinalDestinationArrival] is called instead.
     * If you want to provide different experiences for arriving at different types of waypoints
     * (e. g. regular or EV), you can look up the waypoint type via [RouteLegProgress.legDestination].
     */
    fun onWaypointArrival(routeProgress: RouteProgress)

    /**
     * Called when [MapboxNavigation.navigateNextRouteLeg] has been called and returns true,
     * this observer will be notified of the next route leg start.
     */
    fun onNextRouteLegStart(routeLegProgress: RouteLegProgress)

    /**
     * Triggered when the [RouteProgress.currentState] is equal to [RouteProgressState.COMPLETE],
     * once when there are no more route legs to navigate.
     */
    fun onFinalDestinationArrival(routeProgress: RouteProgress)
}
