package com.mapbox.navigation.route.hybrid

import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.base.route.model.PointNavigation
import com.mapbox.navigation.route.offboard.MapboxOffboardRouter
import com.mapbox.navigation.route.onboard.MapboxOnboardRouter

class MapboxHybridRouter(
    private val onboardRouter: MapboxOnboardRouter,
    private val offboardRouter: MapboxOffboardRouter
) : Router {

    override fun getRoute(
        origin: PointNavigation,
        waypoints: List<PointNavigation>?,
        destination: PointNavigation,
        listener: Router.RouteListener
    ) {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun cancel() {
        onboardRouter.cancel()
        offboardRouter.cancel()
    }
}
