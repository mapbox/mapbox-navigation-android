package com.mapbox.navigation.core.arrival

import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.core.MapboxNavigation

/**
 * When navigating to points of interest, you may want to control the arrival experience.
 * This interface gives you options to control arrival via [MapboxNavigation.setArrivalController].
 *
 * To observe arrival, see [ArrivalObserver]
 */
interface ArrivalController {

    /**
     * Triggered when [RouteProgress.currentState] is equal to [RouteProgressState.COMPLETE].
     *
     * Within your implementation, return true to navigate the next route leg.
     * To manually navigate the next route leg, return false and call [MapboxNavigation.navigateNextRouteLeg].
     *
     * @param routeLegProgress the latest [RouteLegProgress]
     *
     * @return true to automatically call [MapboxNavigation.navigateNextRouteLeg], false to do it manually
     */
    fun navigateNextRouteLeg(routeLegProgress: RouteLegProgress): Boolean
}
