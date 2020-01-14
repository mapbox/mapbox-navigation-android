package com.mapbox.navigation.route.hybrid

import com.mapbox.annotation.navigation.module.MapboxNavigationModule
import com.mapbox.annotation.navigation.module.MapboxNavigationModuleType
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.route.Router

@MapboxNavigationModule(MapboxNavigationModuleType.HybridRouter, skipConfiguration = true)
class MapboxHybridRouter(
    private val onboardRouter: Router,
    private val offboardRouter: Router
) : Router {

    override fun getRoute(
        routeOptions: RouteOptions,
        callback: Router.Callback
    ) = Unit

    override fun cancel() {
        onboardRouter.cancel()
        offboardRouter.cancel()
    }
}
