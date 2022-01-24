package com.mapbox.navigation.core.coroutines.values

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.route.RouterOrigin

data class RoutesWithOrigin(
    val routes: List<DirectionsRoute>,
    val routerOrigin: RouterOrigin
)
