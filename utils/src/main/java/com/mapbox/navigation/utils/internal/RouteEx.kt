package com.mapbox.navigation.utils.internal

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.Point
import com.mapbox.geojson.utils.PolylineUtils

fun RouteLeg.geometryPoints(precision: Int): List<Point> {
    val legGeometry = this.stepsGeometryToPoints(precision)

    return legGeometry.fold(mutableListOf<Point>()) { accumulator, stepPoints ->
        // remove last point: the first point of next step is the same
        accumulator.removeLastOrNull()
        // edge case when step geometry contains only 2 points that might be the same
        // it must be squashed to 1 point
        if (stepPoints.size == 2) {
            accumulator.addAll(stepPoints.toSet())
        } else {
            accumulator.addAll(stepPoints)
        }
        accumulator
    }
}

/**
 * Returns the [Point] at [index] in the leg's geometry without decoding all steps.
 * Decodes one step at a time and stops as soon as the target index is reached.
 */
fun RouteLeg.geometryPointAt(index: Int, precision: Int): Point? {
    var remaining = index
    val stepList = steps() ?: return null
    for (i in stepList.indices) {
        val geometry = stepList[i].geometry() ?: continue
        val points = PolylineUtils.decode(geometry, precision)
        // Mirror geometryPoints() deduplication logic:
        // - First step: contributes all points (or unique set if size == 2)
        // - Subsequent steps: first point is shared with previous step's last, skip it
        val skip = if (i == 0) 0 else 1
        val effective = if (points.size == 2) {
            if (points[0] == points[1]) maxOf(1 - skip, 0) else (2 - skip)
        } else {
            points.size - skip
        }
        if (remaining < effective) return points[skip + remaining]
        remaining -= effective
    }
    return null
}

fun RouteLeg.stepsGeometryToPoints(precision: Int): List<List<Point>> {
    return steps()?.map { step ->
        val geometry = step.geometry() ?: return@map emptyList()
        PolylineUtils.decode(geometry, precision)
    }.orEmpty()
}

/**
 * todo Remove inline references to RouteOptions in favor of taking geometry type as an argument or expose the extensions on top of NavigationRoute instead.
 */
fun DirectionsRoute.precision(): Int =
    precision(routeOptions()?.geometries() ?: DirectionsCriteria.GEOMETRY_POLYLINE6)

/**
 * Geometry polyline (see [DirectionsCriteria.GeometriesCriteria]) to precision const.
 */
fun precision(@DirectionsCriteria.GeometriesCriteria geometry: String): Int {
    return if (geometry == DirectionsCriteria.GEOMETRY_POLYLINE) {
        Constants.PRECISION_5
    } else {
        Constants.PRECISION_6
    }
}
