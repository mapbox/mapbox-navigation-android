package com.mapbox.navigation.core.routealternatives

import com.mapbox.navigation.core.RouteProgressData

internal object AlternativeRouteProgressDataProvider {

    fun getRouteProgressData(
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
            routeGeometryIndex =
                primaryRouteProgressData.routeGeometryIndex - routeGeometryIndexDiff
            val legGeometryIndexDiff =
                primaryFork.geometryIndexInLeg - alternativeFork.geometryIndexInLeg
            legGeometryIndex = primaryRouteProgressData.legGeometryIndex?.let {
                it - legGeometryIndexDiff
            }
        } else {
            legIndex = alternativeFork.legIndex
            routeGeometryIndex = alternativeFork.geometryIndexInRoute
            legGeometryIndex = alternativeFork.geometryIndexInLeg
        }
        return RouteProgressData(legIndex, routeGeometryIndex, legGeometryIndex)
    }
}
