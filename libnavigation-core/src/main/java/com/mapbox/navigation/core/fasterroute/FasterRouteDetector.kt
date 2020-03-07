package com.mapbox.navigation.core.fasterroute

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.trip.model.RouteProgress

internal object FasterRouteDetector {

    /**
     * This threshold optimizes for the current route. For example, if you're 40 minutes
     * away and there is an alternative that is 1 minute faster, it will be ignored.
     */
    private const val PERCENTAGE_THRESHOLD = 0.90

    fun testSameThing(newRoute: DirectionsRoute, routeProgress: RouteProgress): Boolean {
        val newRouteDuration = newRoute.duration() ?: return false
        val weightedDuration = routeProgress.durationRemaining().toDouble() * PERCENTAGE_THRESHOLD
        return newRouteDuration < weightedDuration
    }
    
    fun isRouteFaster(newRoute: DirectionsRoute, routeProgress: RouteProgress): Boolean {
        val newRouteDuration = newRoute.duration() ?: return false
        val weightedDuration = routeProgress.durationRemaining().toDouble() * PERCENTAGE_THRESHOLD
        return newRouteDuration < weightedDuration
    }
}
