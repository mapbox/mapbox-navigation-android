package com.mapbox.navigation.ui.base.api.tripprogress

import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.ui.base.model.tripprogress.TripProgressState

/**
 * An interface for implementing the trip progress API
 */
interface TripProgressApi {

    /**
     * Calculates trip progress data based on a [RouteProgress]
     *
     * @param routeProgress a route progress object
     * @return a trip progress update calculated from a [RouteProgress]
     */
    fun getTripProgress(routeProgress: RouteProgress): TripProgressState.Update
}
