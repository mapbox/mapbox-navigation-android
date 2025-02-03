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
