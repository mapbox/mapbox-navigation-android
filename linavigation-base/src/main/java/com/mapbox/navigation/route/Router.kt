package com.mapbox.navigation.route

import com.mapbox.geojson.Point

interface Router {

    fun getRoute(origin: Point, waypoints: List<Point>, callback: (route: Route) -> Unit)
    fun cancel()
}
