package com.mapbox.navigation.route.offboard

import com.mapbox.geojson.Point
import com.mapbox.navigation.route.Route
import com.mapbox.navigation.route.Router

class MapboxOffboardRouter : Router {
    override fun getRoute(origin: Point, waypoints: List<Point>, callback: (route: Route) -> Unit) {
        TODO("not implemented")
    }

    override fun cancel() {
        TODO("not implemented")
    }
}
