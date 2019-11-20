package com.mapbox.navigation.route.onboard

import com.mapbox.annotation.navigation.module.MapboxNavigationModule
import com.mapbox.annotation.navigation.module.MapboxNavigationModuleType
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.navigator.MapboxNativeNavigator

@MapboxNavigationModule(MapboxNavigationModuleType.OnboardRouter, skipConfiguration = true)
class MapboxOnboardRouter(private val navigator: MapboxNativeNavigator) : Router {

    override fun getRoute(
        origin: Point,
        waypoints: List<Point>?,
        destination: Point,
        callback: Router.Callback
    ) = Unit

    override fun cancel() = Unit

    class Config {
        fun compile(): String = TODO("not implemented")
    }
}
