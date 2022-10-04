package com.mapbox.navigation.ui.maps.internal.route.line

import com.mapbox.geojson.Point
import java.util.function.Supplier

class PointProvider(private val p: Point): Supplier<Point> {
    override fun get(): Point {
        return p
    }
}
