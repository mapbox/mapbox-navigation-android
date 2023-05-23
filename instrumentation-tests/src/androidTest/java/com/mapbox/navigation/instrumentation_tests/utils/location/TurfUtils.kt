package com.mapbox.navigation.instrumentation_tests.utils.location

import com.mapbox.geojson.Point
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement

internal operator fun Int.compareTo(pointsDiff: Pair<Point, Point>): Int {
    val (p1, p2) = pointsDiff
    return this - TurfMeasurement.distance(p1, p2, TurfConstants.UNIT_METERS).toInt()
}
