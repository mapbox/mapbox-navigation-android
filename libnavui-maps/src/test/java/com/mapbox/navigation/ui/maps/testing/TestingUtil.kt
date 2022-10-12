package com.mapbox.navigation.ui.maps.testing

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.route.toNavigationRoute
import com.mapbox.navigation.testing.FileUtils
import org.json.JSONObject

object TestingUtil {
    fun loadRoute(routeFileName: String, uuid: String? = null): DirectionsRoute {
        val routeAsJson = FileUtils.loadJsonFixture(routeFileName)
        val options = JSONObject(routeAsJson).let {
            if (it.has("routeOptions")) {
                RouteOptions.fromJson(it.getJSONObject("routeOptions").toString())
            } else {
                null
            }
        }
        return DirectionsRoute.fromJson(routeAsJson, options, uuid)
    }

    fun loadNavigationRoute(routeFileName: String, uuid: String? = null) =
        loadRoute(routeFileName, uuid).toNavigationRoute(RouterOrigin.Offboard)
}
