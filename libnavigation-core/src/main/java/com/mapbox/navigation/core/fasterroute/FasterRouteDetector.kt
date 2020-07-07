package com.mapbox.navigation.core.fasterroute

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.trip.model.RouteProgress

internal class FasterRouteDetector(
    private val routeComparator: RouteComparator
) {

    fun isRouteFaster(alternativeRoute: DirectionsRoute, routeProgress: RouteProgress): Boolean {
        val alternativeDuration = alternativeRoute.duration() ?: return false
        val weightedDuration = routeProgress.durationRemaining * PERCENTAGE_THRESHOLD
        val isNewRouteFaster = alternativeDuration < weightedDuration

        // TODO Moved here for debugging, don't merge this
        val isNewRoute = routeComparator.isNewRoute(routeProgress, alternativeRoute)
        return isNewRouteFaster && isNewRoute
    }

    companion object {
        /**
         * This threshold optimizes for the current route. For example, if you're 40 minutes
         * away and there is an alternative that is 1 minute faster, it will be ignored.
         */
        private const val PERCENTAGE_THRESHOLD = 0.90
    }
}
