package com.mapbox.navigation.ui.maps

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.ui.base.MapboxAction

sealed class RouteLineAction: MapboxAction {
    class UpdateDistanceRemaining(val distanceRemaining: Float, val route: DirectionsRoute): RouteLineAction()
}
