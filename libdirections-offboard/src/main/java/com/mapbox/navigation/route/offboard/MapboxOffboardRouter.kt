package com.mapbox.navigation.route.offboard

import com.mapbox.geojson.Point
import com.mapbox.navigation.base.route.Route
import com.mapbox.navigation.base.route.Router

class MapboxOffboardRouter : Router {

    override fun getRoute(origin: Point, waypoints: List<Point>, callback: (route: Route) -> Unit) {
        TODO("not implemented")
    }

    override fun getRoute(origin: Point, waypoints: List<Point>, listener: Router.RouteListener) {
    }

    override fun cancel() {
        TODO("not implemented")
    }
}
