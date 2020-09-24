package com.mapbox.navigation.ui.maps

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.ui.base.MapboxResult

sealed class RouteLineResult: MapboxResult {
    class UpdateDistanceRemaining(val distanceRemaining: Float, val route: DirectionsRoute): RouteLineResult()
    class Filler: RouteLineResult()
}
