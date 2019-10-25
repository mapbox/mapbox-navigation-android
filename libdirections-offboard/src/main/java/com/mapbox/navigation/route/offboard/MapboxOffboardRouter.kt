package com.mapbox.navigation.route.offboard

import com.mapbox.annotation.navigation.module.MapboxNavigationModule
import com.mapbox.annotation.navigation.module.MapboxNavigationModuleType
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.route.Route
import com.mapbox.navigation.base.route.Router

@MapboxNavigationModule(MapboxNavigationModuleType.OffboardRouter, skipConfiguration = true)
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
