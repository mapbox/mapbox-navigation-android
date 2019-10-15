package com.mapbox.navigation.route.onboard

import com.mapbox.geojson.Point
import com.mapbox.navigation.route.Route
import com.mapbox.navigation.route.Router
import com.mapbox.navigator.Navigator

class MapboxOnboardRouter(private val navigator: Navigator) : Router {

    override fun getRoute(origin: Point, waypoints: List<Point>, callback: (route: Route) -> Unit) {
        TODO("not implemented")
    }

    override fun cancel() {
        TODO("not implemented")
    }

    class Config {
        fun compile(): String = TODO("not implemented")
    }
}
