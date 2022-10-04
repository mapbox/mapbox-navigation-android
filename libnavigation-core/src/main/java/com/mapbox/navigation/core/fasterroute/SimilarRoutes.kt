package com.mapbox.navigation.core.fasterroute

import com.mapbox.geojson.Point
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.utils.DecodeUtils.completeGeometryToPoints
import com.mapbox.turf.TurfMeasurement

internal fun calculateDescriptionSimilarity(a: NavigationRoute, b: NavigationRoute): Double {
    val firstSummary = parseSummaries(a)
    val secondSummary = parseSummaries(b)
    return calculateSimilarityOfSets(firstSummary, secondSummary) { it.size.toDouble() }
}

internal fun calculateGeometrySimilarity(a: NavigationRoute, b: NavigationRoute): Double {
    if (a.id == b.id) return 1.0
    val (shorter, longer) = if (a.directionsRoute.distance() > b.directionsRoute.distance()) {
        Pair(b, a)
    } else {
        Pair(a, b)
    }

    val shorterRouteSegments = toSegments(shorter)
    val longerRouteSegments = toSegments(longer)
    return calculateSimilarityOfSets(shorterRouteSegments, longerRouteSegments) { set ->
        set.sumOf { it.length }
    }
}

private fun parseSummaries(route: NavigationRoute) =
    route.directionsRoute.legs()
        .orEmpty()
        .mapNotNull { it.summary() }
        .map { it.split(", ") }
        .flatten()
        .toSet()

private fun <T> calculateSimilarityOfSets(
    a: Set<T>,
    b: Set<T>,
    aggregator: (Set<T>) -> Double
): Double {
    val diff = a.toMutableSet().apply {
        removeAll(b)
    }
    return (1.0 - (aggregator(diff) / aggregator(a)))
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
