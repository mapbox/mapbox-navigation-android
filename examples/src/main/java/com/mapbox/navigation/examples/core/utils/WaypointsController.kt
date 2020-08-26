package com.mapbox.navigation.examples.core.utils

import android.location.Location
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.navigation.examples.utils.extensions.toPoint

class WaypointsController {
    val waypoints = mutableListOf<Point>()

    fun add(latLng: LatLng) {
        waypoints.add(latLng.toPoint())
    }

    fun clear() {
        waypoints.clear()
    }

    fun coordinates(originLocation: Location): List<Point> {
        val coordinates = mutableListOf<Point>()
        coordinates.add(originLocation.toPoint())
        coordinates.addAll(waypoints)
        return coordinates
    }
}
