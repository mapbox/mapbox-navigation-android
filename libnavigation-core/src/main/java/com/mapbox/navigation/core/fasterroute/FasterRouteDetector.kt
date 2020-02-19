package com.mapbox.navigation.core.fasterroute

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.trip.model.RouteProgress

internal object FasterRouteDetector {

    fun isRouteFaster(newRoute: DirectionsRoute, routeProgress: RouteProgress): Boolean {
        return false
    }
}
