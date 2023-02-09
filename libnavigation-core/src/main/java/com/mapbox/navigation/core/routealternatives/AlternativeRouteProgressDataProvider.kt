package com.mapbox.navigation.core.routealternatives

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.utils.DecodeUtils.stepsGeometryToPoints
import com.mapbox.navigation.core.RouteProgressData
import com.mapbox.navigation.utils.internal.ThreadController
import kotlinx.coroutines.withContext

internal object AlternativeRouteProgressDataProvider {

    suspend fun getRouteProgressData(
        primaryRouteProgressData: RouteProgressData,
        alternativeMetadata: AlternativeRouteMetadata
    ): RouteProgressData {
        val legIndex: Int
        val routeGeometryIndex: Int
        val legGeometryIndex: Int?
        val primaryFork = alternativeMetadata.forkIntersectionOfPrimaryRoute
        val alternativeFork = alternativeMetadata.forkIntersectionOfAlternativeRoute
        val isForkPointAheadOnPrimaryRoute =
            primaryRouteProgressData.routeGeometryIndex < primaryFork.geometryIndexInRoute
        if (isForkPointAheadOnPrimaryRoute) {
            val legIndexDiff = primaryFork.legIndex - alternativeFork.legIndex
            legIndex = primaryRouteProgressData.legIndex - legIndexDiff
            val routeGeometryIndexDiff =
                primaryFork.geometryIndexInRoute - alternativeFork.geometryIndexInRoute
            val alternativeStarted =
                primaryRouteProgressData.routeGeometryIndex >= routeGeometryIndexDiff
            routeGeometryIndex = if (alternativeStarted) {
                primaryRouteProgressData.routeGeometryIndex - routeGeometryIndexDiff
            } else {
                0
            }
            legGeometryIndex = routeGeometryIndex -
                prevLegsGeometryIndicesCount(alternativeMetadata.navigationRoute, legIndex)
        } else {
            legIndex = alternativeFork.legIndex
            routeGeometryIndex = alternativeFork.geometryIndexInRoute
            legGeometryIndex = alternativeFork.geometryIndexInLeg
        }
        return RouteProgressData(legIndex, routeGeometryIndex, legGeometryIndex)
    }

    private suspend fun prevLegsGeometryIndicesCount(
        route: NavigationRoute,
        currentLegIndex: Int
    ): Int {
        return withContext(ThreadController.DefaultDispatcher) {
            var result = 0
            val stepsGeometries by lazy { route.directionsRoute.stepsGeometryToPoints() }
            for (legIndex in 0 until currentLegIndex) {
                val legGeometries = stepsGeometries.getOrNull(legIndex)
                if (legGeometries != null) {
                    // remove step duplicates (the last point in prev step is the same as the first point in the current step)
                    result += legGeometries.sumOf { it.size } - legGeometries.size + 1
                }
            }
            // remove leg duplicates (the last point of the last step of prev leg is the same as the first point of the first step in the current leg)
            result -= currentLegIndex
            result
        }
    }
}
