package com.mapbox.navigation.base.route

import com.mapbox.geojson.Point
import com.mapbox.navigation.base.model.route.Route

interface Router {

    fun getRoute(origin: Point, waypoints: List<Point>, callback: (route: Route) -> Unit)
    fun cancel()
}
