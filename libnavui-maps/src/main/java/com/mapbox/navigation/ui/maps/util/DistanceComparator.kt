package com.mapbox.navigation.ui.maps.util

import com.mapbox.geojson.Point
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement
import java.util.function.Supplier

class DistanceComparator(private val origin: Point): Comparator<Supplier<Point>> {

    override fun compare(p0: Supplier<Point>, p1: Supplier<Point>): Int {
        return TurfMeasurement.distance(origin, p0.get(), TurfConstants.UNIT_METERS)
            .compareTo(TurfMeasurement.distance(origin, p1.get(), TurfConstants.UNIT_METERS))
    }
}
