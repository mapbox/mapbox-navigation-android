package com.mapbox.navigation.base.fasterroute

import com.mapbox.api.directions.v5.models.DirectionsRoute

interface FasterRouteObserver {

    fun onFasterRouteAvailable(fasterRoute: DirectionsRoute)

    fun onFasterRouteNotFound()
}
