package com.mapbox.navigation.core.arrival

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
     * this observer will be notified of the next route leg start.
     */
    fun onNextRouteLegStart(routeLegProgress: RouteLegProgress)

    /**
     * This will be called once the [RouteProgress.currentState] has reached [RouteProgressState.ROUTE_COMPLETE].
     * This means the device has reached the final destination on the route.
     */
    fun onFinalDestinationArrival(routeProgress: RouteProgress)
}
