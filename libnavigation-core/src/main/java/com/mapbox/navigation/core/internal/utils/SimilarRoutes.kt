package com.mapbox.navigation.core.internal.utils

import com.mapbox.geojson.Point
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.utils.DecodeUtils.completeGeometryToPoints

fun calculateRoutesSimilarity(a: NavigationRoute, b: NavigationRoute): Double {
    if (a.id == b.id) return 1.0

    val aSegments = toSegments(a)
    val bSegments = toSegments(b)
    bSegments.removeAll(aSegments)
    return bSegments.size.toDouble() / aSegments.size
}

private fun toSegments(a: NavigationRoute): MutableSet<Segment> {
    val points = a.directionsRoute.completeGeometryToPoints()
    val segments = mutableSetOf<Segment>()
    var previousPoint: Point? = null
    for (point in points) {
        if (previousPoint == null) {
            previousPoint = point
            continue
        }

        segments.add(Segment(previousPoint, point))
        previousPoint = point
    }
    return segments
}

private data class Segment(val from: Point, val to: Point)