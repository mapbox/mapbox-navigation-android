package com.mapbox.navigation.ui.maps.testing

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.testing.FileUtils

object TestingUtil {
    fun loadRoute(routeFileName: String): DirectionsRoute {
        val routeAsJson = FileUtils.loadJsonFixture(routeFileName)
        return DirectionsRoute.fromJson(routeAsJson)
    }
}
