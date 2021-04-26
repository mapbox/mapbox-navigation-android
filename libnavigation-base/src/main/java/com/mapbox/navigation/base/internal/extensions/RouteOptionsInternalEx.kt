@file:JvmName("RouteOptionsInternalEx")

package com.mapbox.navigation.base.internal.extensions

import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point

/**
 * Takes a list of [Point]s and correctly adds them as waypoints in the correct order.
 *
 * @receiver RouteOptions.Builder
 * @param origin Point
 * @param waypoints List<Point?>?
 * @param destination Point
 * @return RouteOptions.Builder
 */
@JvmOverloads
fun RouteOptions.Builder.coordinates(
    origin: Point,
    waypoints: List<Point?>? = null,
    destination: Point
): RouteOptions.Builder {
    val coordinates = mutableListOf<Point>().apply {
        add(origin)
        waypoints?.filterNotNull()?.forEach { add(it) }
        add(destination)
    }

    coordinates(coordinates)

    return this
}
