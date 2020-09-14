package com.mapbox.navigation.core.fasterroute

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.utils.internal.ThreadController
import kotlinx.coroutines.withContext

internal class FasterRouteDetector(
    private val routeComparator: RouteComparator
) {

    suspend fun isRouteFaster(
        alternativeRoute: DirectionsRoute,
        routeProgress: RouteProgress
    ): Boolean = withContext(ThreadController.IODispatcher) {
        val alternativeDuration = alternativeRoute.duration()
        val weightedDuration = routeProgress.durationRemaining * PERCENTAGE_THRESHOLD
        val isRouteFaster = alternativeDuration < weightedDuration
        return@withContext isRouteFaster && routeComparator.isRouteDescriptionDifferent(
            routeProgress,
            alternativeRoute
        )
    }

    companion object {
        /**
         * This threshold optimizes for the current route. For example, if you're 40 minutes
         * away and there is an alternative that is 1 minute faster, it will be ignored.
         */
        private const val PERCENTAGE_THRESHOLD = 0.90
    }
}
