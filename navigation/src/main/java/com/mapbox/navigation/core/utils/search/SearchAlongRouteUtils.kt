package com.mapbox.navigation.core.utils.search

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.StepIntersection
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.utils.internal.takeEvenly
import com.mapbox.turf.TurfTransformation

/**
 * Utility class designed to optimize search along a route by providing
 * optimal points for this scenario. This reduces network usage by limiting
 * the number of points sent to the API (which can reach several thousand on long routes)
 * and enhances search results for APIs lacking detailed road graph data.
 *
 * Example statistics for different routes:
 *
 * **Oslo – Malaga (3600 km):**
 *  - Original: 43,666 points
 *  - Optimized with [SearchAlongRouteUtils]: 8,230 points
 *  - Optimized with [TurfTransformation] (tolerance 0.0): 43,647 points
 *  - Optimized with [TurfTransformation] (tolerance 0.01): 411 points
 *
 * **Paris – Quimper (564 km):**
 *  - Original: 7,081 points
 *  - Optimized with [SearchAlongRouteUtils]: 1,282 points
 *  - Optimized with [TurfTransformation] (tolerance 0.0): 7,079 points
 *  - Optimized with [TurfTransformation] (tolerance 0.01): 55 points
 *
 * **London Brixton – London Harlesden (16 km):**
 *  - Original: 903 points
 *  - Optimized with [SearchAlongRouteUtils]: 469 points
 *  - Optimized with [TurfTransformation] (tolerance 0.0): 903 points
 *  - Optimized with [TurfTransformation] (tolerance 0.01): 3 points
 */
@ExperimentalMapboxNavigationAPI
object SearchAlongRouteUtils {

    /**
     * Selects points starting from the current progress position to the end of the route.
     *
     * @param progress The current route progress.
     * @param limit The maximum number of points to return, selected evenly along the route.
     * If null, no limit is applied (default).
     *
     * @return A list of selected points, or null if the required data cannot be retrieved.
     */
    fun selectPoints(progress: RouteProgress, limit: Int? = null): List<Point> {
        val currentLegProgress = progress.currentLegProgress
        val currentStepProgress = currentLegProgress?.currentStepProgress ?: return emptyList()

        return selectPoints(
            route = progress.route,
            legIndex = currentLegProgress.legIndex,
            legStepIndex = currentStepProgress.stepIndex,
            legStepIntersectionIndex = currentStepProgress.intersectionIndex,
            limit = limit,
        )
    }

    /**
     * Selects points starting from a specified leg and step index to the end of the route.
     *
     * @param route The route from which to select points.
     * @param legIndex The starting leg index for point selection. Default is 0.
     * @param legStepIndex The starting step index within the specified with [legIndex] leg.
     * Default is 0.
     * @param legStepIntersectionIndex The starting intersection index within the specified with
     * [legIndex] and [legStepIndex] step. Default is 0.
     * @param limit The maximum number of points to return, selected evenly along the route.
     * If null, no limit is applied (default).
     *
     * @return A list of selected points, or null if the required data cannot be retrieved.
     */
    fun selectPoints(
        route: DirectionsRoute,
        legIndex: Int = 0,
        legStepIndex: Int = 0,
        legStepIntersectionIndex: Int = 0,
        limit: Int? = null,
    ): List<Point> {
        val legs = route.legs() ?: return emptyList()
        return legs.asSequence()
            .drop(legIndex)
            .map { it.steps()?.asSequence() ?: emptySequence() }
            .flatten()
            .drop(legStepIndex)
            .map { it.intersections()?.asSequence() ?: emptySequence() }
            .flatten()
            .drop(legStepIntersectionIndex)
            .filter { it.isApplicableForSar() }
            .map { intersection -> intersection.location() }
            .toList()
            .run {
                if (limit == null) {
                    this
                } else {
                    takeEvenly(limit)
                }
            }
    }

    /**
     * Tunnels and ferries are excluded because there are unlikely to be any POIs around
     * those locations. Even if a tunnel or ferry point is located just before or after the
     * tunnel or ferry itself, it is often a location that is not convenient for navigation,
     * or an areas with restricted access (like ports).
     * At the same time, there is likely to be an intersection within the city, located before
     * or after the tunnel or ferry, which would be a more suitable proximity point
     * for search purposes.
     */
    private fun StepIntersection.isApplicableForSar(): Boolean {
        val classes = classes() ?: return true
        return !classes.contains("tunnel") && !classes.contains("ferry")
    }
}
