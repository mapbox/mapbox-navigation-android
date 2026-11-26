package com.mapbox.navigation.base.internal.route

import androidx.annotation.RestrictTo
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.DirectionsRouteFBWrapper
import com.mapbox.api.directions.v5.models.utils.FlatbuffersListWrapper
import com.mapbox.directions.accessors.FbBuffer
import com.mapbox.directions.accessors.route_request.FbIntersectionDistances
import com.mapbox.directions.accessors.route_request.FbRoute
import com.mapbox.geojson.Point
import com.mapbox.navigation.utils.internal.logE
import java.nio.ByteBuffer
import java.nio.ByteOrder

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
            val b = nativeComputationResult.buffer.toFbBuffer()
            FbIntersectionDistances(b.root()).intersectionsDistancesAsListsOfDouble(b)
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
            val b = nativeComputationResult.buffer.toFbBuffer()
            FbRoute(b.root()).toListsOfPoints(b)
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

private fun ByteBuffer.toFbBuffer() = FbBuffer(order(ByteOrder.LITTLE_ENDIAN))

private fun FbBuffer.root(): Int = indirect(byteBuffer.position())

private fun FbRoute.toListsOfPoints(b: FbBuffer): List<List<List<Point>>> {
    val legs = legs(b)
    return FlatbuffersListWrapper.get(legs.size) { legIndex ->
        val steps = legs.get(b, legIndex).steps(b)
        FlatbuffersListWrapper.get(steps.size) { stepIndex ->
            val geometry = steps.get(b, stepIndex).geometry(b)
            FlatbuffersListWrapper.get<Point>(geometry.size) { geometryIndex ->
                val point = geometry.get(b, geometryIndex)
                Point.fromLngLat(point.longitude(b), point.latitude(b))
            }.orEmpty()
        }.orEmpty()
    }.orEmpty()
}

private fun FbIntersectionDistances.intersectionsDistancesAsListsOfDouble(
    b: FbBuffer,
): List<List<Double>> {
    val legs = legs(b)
    return FlatbuffersListWrapper.get(legs.size) { legIndex ->
        val steps = legs.get(b, legIndex).steps(b)
        FlatbuffersListWrapper.get(steps.size) { stepIndex ->
            steps.get(b, stepIndex)
        }.orEmpty()
    }.orEmpty()
}
