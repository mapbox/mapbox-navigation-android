package com.mapbox.navigation.qa_test_app.utils

import com.mapbox.geojson.Point

class WaypointsController {
    private val waypoints = mutableListOf<Point>()

    fun add(point: Point) {
        waypoints.add(point)
    }

    fun clear() {
        waypoints.clear()
    }

    fun coordinates(origin: Point): List<Point> {
        val coordinates = mutableListOf<Point>()
        coordinates.add(origin)
        coordinates.addAll(waypoints)
        return coordinates
    }
}
