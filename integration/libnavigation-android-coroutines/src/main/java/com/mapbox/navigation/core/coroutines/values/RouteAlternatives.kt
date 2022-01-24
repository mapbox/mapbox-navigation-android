package com.mapbox.navigation.core.coroutines.values

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.trip.model.RouteProgress

data class RouteAlternatives(
    val routeProgress: RouteProgress,
    val alternatives: List<DirectionsRoute>,
    val routerOrigin: RouterOrigin
)
