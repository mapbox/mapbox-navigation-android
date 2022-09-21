package com.mapbox.navigation.ui.maps.testing

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.route.toNavigationRoute
import com.mapbox.navigation.testing.FileUtils

object TestingUtil {
    fun loadRoute(routeFileName: String, uuid: String? = null): DirectionsRoute {
        val routeAsJson = FileUtils.loadJsonFixture(routeFileName)
        return DirectionsRoute.fromJson(routeAsJson).run {
            if (uuid != null) {
                this.toBuilder().requestUuid(uuid).build()
            } else {
                this
            }
        }
    }

    fun loadNavigationRoute(routeFileName: String, uuid: String? = null) =
        loadRoute(routeFileName, uuid).toNavigationRoute(RouterOrigin.Offboard)
}
