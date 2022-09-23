package com.mapbox.navigation.core.internal.utils

import com.mapbox.geojson.Point
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.utils.DecodeUtils.completeGeometryToPoints
import com.mapbox.turf.TurfMeasurement

fun calculateSimilarity(a: NavigationRoute, b: NavigationRoute): Double {
    if (a.id == b.id) return 1.0

    val aSegments = toSegments(a)
    val bSegments = toSegments(b)
    val diff = aSegments.toMutableSet().apply {
        removeAll(bSegments)
    }
    return (1.0 - (diff.sumOf { it.length } / aSegments.sumOf { it.length }))
}

private fun toSegments(a: NavigationRoute): MutableSet<Segment> {
    val points = a.directionsRoute.completeGeometryToPoints()
    val segments = mutableSetOf<Segment>()
    var previousPoint: Point? = null
    for (point in points.drop(1).dropLast(1)) {
        if (previousPoint == null) {
            previousPoint = point
            continue
        }

        segments.add(Segment(previousPoint, point))
        previousPoint = point
    }
    return segments
}

private data class Segment(val from: Point, val to: Point) {
    val length get() = TurfMeasurement.distance(from, to)
}