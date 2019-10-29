package com.mapbox.navigation.route.hybrid

import com.mapbox.geojson.Point
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.base.route.model.Route
import com.mapbox.navigation.route.offboard.MapboxOffboardRouter
import com.mapbox.navigation.route.onboard.MapboxOnboardRouter

class MapboxHybridRouter(
    private val onboardRouter: MapboxOnboardRouter,
    private val offboardRouter: MapboxOffboardRouter = MapboxOffboardRouter()
) : Router {

    override fun getRoute(origin: Point, waypoints: List<Point>, callback: (route: Route) -> Unit) {
        TODO("not implemented")
    }

    override fun cancel() {
        onboardRouter.cancel()
        offboardRouter.cancel()
    }
}
