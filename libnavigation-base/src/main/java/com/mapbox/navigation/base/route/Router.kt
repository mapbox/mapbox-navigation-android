package com.mapbox.navigation.base.route

import com.mapbox.geojson.Point
import com.mapbox.navigation.base.route.model.Route

interface Router {

    fun getRoute(
        origin: Point,
        waypoints: List<Point> = emptyList(),
        destination: Point,
        callback: Callback
    )

    fun cancel()

    interface Callback {
        fun onResponse(routes: List<Route>)

        fun onFailure(throwable: Throwable)
    }
}
