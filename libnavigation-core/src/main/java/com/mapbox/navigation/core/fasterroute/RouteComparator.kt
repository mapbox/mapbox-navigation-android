package com.mapbox.navigation.core.fasterroute

import android.util.Log
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.trip.model.RouteProgress

/**
 * Compares if an alternative route is different from the current route.
 */
internal class RouteComparator {

    private val legacyRouteComparator = LegacyRouteComparator()

    /**
     * @param routeProgress current route progress
     * @param alternativeRoute suggested new route
     *
     * @return true when the alternative route has different
     * geometry from the current route progress
     */
    fun isNewRoute(routeProgress: RouteProgress, alternativeRoute: DirectionsRoute): Boolean {
        val compareValue = legacyRouteComparator.compareRoutes(routeProgress.route, alternativeRoute)

        Log.i("faster_route_debug","faster_route_debug route compare $compareValue")

        val currentGeometry = routeProgress.route.geometry() ?: ""
        val alternativeGeometry = (alternativeRoute.geometry() ?: "").ifEmpty {
            return false
        }

        return isNewRoute(currentGeometry, alternativeGeometry)
    }

    private fun isNewRoute(currentGeometry: String, alternativeGeometry: String): Boolean {
        return currentGeometry != alternativeGeometry
    }
}
