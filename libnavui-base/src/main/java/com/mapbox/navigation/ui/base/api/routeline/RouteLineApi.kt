package com.mapbox.navigation.ui.base.api.routeline

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.ui.base.model.routeline.RouteLineState

interface RouteLineApi {

    fun updateDistanceRemaining(distanceRemaining: Float, route: DirectionsRoute, routeLineState: RouteLineState): RouteLineState
}


class RouteBiz: RouteLineApi {
    override fun updateDistanceRemaining(distanceRemaining: Float, route: DirectionsRoute, routeLineState: RouteLineState): RouteLineState {
        routeLineState.updateDistanceRemaining(distanceRemaining, route)
        return  routeLineState
    }
}
