package com.mapbox.navigation.core.fasterroute

import com.mapbox.api.directions.v5.models.DirectionsRoute

interface FasterRouteObserver {

    fun onFasterRouteAvailable(fasterRoute: DirectionsRoute)
}
