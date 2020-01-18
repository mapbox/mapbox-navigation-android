package com.mapbox.navigation.base.fasterroute

import com.mapbox.api.directions.v5.models.DirectionsRoute

interface FasterRouteListener {

    fun onFasterRouteAvailable(fasterRoute: DirectionsRoute)

    fun onFasterRouteNotFound()
}
