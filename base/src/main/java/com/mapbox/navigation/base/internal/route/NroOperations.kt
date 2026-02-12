package com.mapbox.navigation.base.internal.route

import androidx.annotation.RestrictTo
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.DirectionsRouteFBWrapper
import com.mapbox.api.directions.v5.models.utils.FlatbuffersListWrapper
import com.mapbox.directions.generated.route_request.IntersectionDistances
import com.mapbox.directions.generated.route_request.Route
import com.mapbox.geojson.Point
import com.mapbox.navigation.utils.internal.logE

private const val LOG_CATEGORY = "NRO-OPERATIONS"

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun DirectionsRoute.getIntersectionsDistancesFromNroOrNull(
    minimumMetersForIntersectionDensity: Double,
): List<List<Double>>? {
    return if (this is DirectionsRouteFBWrapper) {
        try {
            val nativeComputationResult = this.context.getIntersectionsDistances(
                minimumMetersForIntersectionDensity,
            )
            IntersectionDistances.getRootAsIntersectionDistances(nativeComputationResult.buffer)
                .intersectionsDistancesAsListsOfDouble()
        } catch (t: Throwable) {
            logE(LOG_CATEGORY) {
                "failed to compute intersections distances, returning null. " +
                    "error: ${t.message}"
            }
            null
        }
    } else {
        null
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun DirectionsRoute.getCompoundManeuverGeometryPointsFromNroOrNull(
    distanceToCoalesceCompoundManeuvers: Double,
    distanceToFrameAfterManeuver: Double,
): List<List<List<Point>>>? {
    return if (this is DirectionsRouteFBWrapper) {
        try {
            val nativeComputationResult = this.context.getCompoundManeuverGeometryPoints(
                distanceToCoalesceCompoundManeuvers,
                distanceToFrameAfterManeuver,
            )
            Route.getRootAsRoute(nativeComputationResult.buffer).toListsOfPoints()
        } catch (t: Throwable) {
            logE(LOG_CATEGORY) {
                "failed to compute compound maneuver geometry points, returning null. " +
                    "error: ${t.message}"
            }
            null
        }
    } else {
        null
    }
}

private fun Route.toListsOfPoints(): List<List<List<Point>>> =
    FlatbuffersListWrapper.get(legsLength) { legIndex ->
        val leg = legs(legIndex)!!
        FlatbuffersListWrapper.get(leg.stepsLength) { stepIndex ->
            val step = leg.steps(stepIndex)!!
            FlatbuffersListWrapper.get<Point>(step.geometryLength) { geometryIndex ->
                val point = step.geometry(geometryIndex)!!
                Point.fromLngLat(point.longitude, point.latitude)
            }.orEmpty()
        }.orEmpty()
    }.orEmpty()

private fun IntersectionDistances.intersectionsDistancesAsListsOfDouble(): List<List<Double>> =
    FlatbuffersListWrapper.get(legsLength) {
        val stepContainer = legs(it)!!
        FlatbuffersListWrapper.get(stepContainer.stepsLength) {
            stepContainer.steps(it)
        }.orEmpty()
    }.orEmpty()
