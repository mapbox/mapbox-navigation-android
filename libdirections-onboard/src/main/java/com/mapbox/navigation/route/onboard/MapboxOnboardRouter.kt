package com.mapbox.navigation.route.onboard

import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.base.route.model.PointNavigation
import com.mapbox.navigation.navigator.MapboxNativeNavigator

class MapboxOnboardRouter(private val navigator: MapboxNativeNavigator) : Router {

    override fun getRoute(
        origin: PointNavigation,
        waypoints: List<PointNavigation>?,
        destination: PointNavigation,
        listener: Router.RouteListener
    ) {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun cancel() {
        TODO("not implemented")
    }

    class Config {
        fun compile(): String = TODO("not implemented")
    }
}
