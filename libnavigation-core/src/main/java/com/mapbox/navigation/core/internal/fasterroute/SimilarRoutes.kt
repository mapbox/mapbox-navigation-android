package com.mapbox.navigation.core.internal.fasterroute

import com.mapbox.geojson.Point
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.utils.DecodeUtils.completeGeometryToPoints
import com.mapbox.turf.TurfMeasurement

internal fun calculateDescriptionSimilarity(a: NavigationRoute, b: NavigationRoute): Double {
    val firstSummary = parseSummaries(a)
    val secondSummary = parseSummaries(b)
    return calculateSimilarityOfSets(firstSummary, secondSummary) { it.size.toDouble() }
}

private fun parseSummaries(route: NavigationRoute) =
    route.directionsRoute.legs()
        .orEmpty()
        .mapNotNull { it.summary() }
        .map { it.split(", ") }
        .flatten()
        .toSet()

internal fun calculateDescriptionLevensteinSimilarity(a: NavigationRoute, b: NavigationRoute): Double {
    if (a.id == b.id) return 1.0
    val (shorter, longer) = if (a.directionsRoute.distance() > b.directionsRoute.distance()) {
        Pair(b, a)
    } else Pair(a, b)

    val shortedDescription = buildDescription(shorter)
    val longerDescription = buildDescription(longer)
    val distance = levenshtein(shortedDescription, longerDescription)
    return 1.0 - distance.toDouble() / longerDescription.length
}

private fun buildDescription(route: NavigationRoute) =
    route.directionsRoute.legs()?.joinToString { it.summary() ?: "-" } ?: ""

private fun levenshtein(s: String, t: String): Int {
    // degenerate cases
    if (s == t)  return 0
    if (s == "") return t.length
    if (t == "") return s.length

    // create two integer arrays of distances and initialize the first one
    val v0 = IntArray(t.length + 1) { it }  // previous
    val v1 = IntArray(t.length + 1)         // current

    var cost: Int
    for (i in 0 until s.length) {
        // calculate v1 from v0
        v1[0] = i + 1
        for (j in 0 until t.length) {
            cost = if (s[i] == t[j]) 0 else 1
            v1[j + 1] = Math.min(v1[j] + 1, Math.min(v0[j + 1] + 1, v0[j] + cost))
        }
        // copy v1 to v0 for next iteration
        for (j in 0 .. t.length) v0[j] = v1[j]
    }
    return v1[t.length]
}

internal fun calculateGeometrySimilarity(a: NavigationRoute, b: NavigationRoute): Double {
    if (a.id == b.id) return 1.0
    val (shorter, longer) = if (a.directionsRoute.distance() > b.directionsRoute.distance()) {
        Pair(b, a)
    } else Pair(a, b)

    val shorterRouteSegments = toSegments(shorter)
    val longerRouteSegments = toSegments(longer)
    return calculateSimilarityOfSets(shorterRouteSegments, longerRouteSegments) { it.sumOf { it.length } }
}

private fun <T> calculateSimilarityOfSets(
    a: Set<T>,
    b: Set<T>,
    aggregator: (Set<T>)->Double
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
    for (point in points.drop(1).dropLast(1)) { // TODO: also remove silent waypoints
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