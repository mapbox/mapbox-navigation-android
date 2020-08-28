package com.mapbox.navigation.core.fasterroute

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.navigation.base.trip.model.RouteProgress

/**
 * Compares if an alternative route is different from the current route progress.
 */
internal class RouteComparator {

    private val mapLegStepToName: (LegStep) -> String = { it.name() ?: "" }

    /**
     * @param routeProgress current route progress
     * @param alternativeRoute suggested new route
     *
     * @return true when the alternative route has different
     * geometry from the current route progress
     */
    fun isRouteDescriptionDifferent(
        routeProgress: RouteProgress,
        alternativeRoute: DirectionsRoute
    ): Boolean {
        val currentDescription = routeDescription(routeProgress)
        val alternativeDescription = alternativeDescription(alternativeRoute)
        alternativeDescription.ifEmpty {
            return false
        }

        return currentDescription != alternativeDescription
    }

    private fun routeDescription(routeProgress: RouteProgress): String {
        val routeLeg = routeProgress.currentLegProgress?.routeLeg
        val steps = routeLeg?.steps()
        val stepIndex = routeProgress.currentLegProgress?.currentStepProgress?.stepIndex
            ?: return ""

        return steps?.listIterator(stepIndex)?.asSequence()
            ?.joinToString(transform = mapLegStepToName) ?: ""
    }

    private fun alternativeDescription(directionsRoute: DirectionsRoute): String {
        val steps = directionsRoute.legs()?.firstOrNull()?.steps()
        return steps?.asSequence()
            ?.joinToString(transform = mapLegStepToName) ?: ""
    }
}
