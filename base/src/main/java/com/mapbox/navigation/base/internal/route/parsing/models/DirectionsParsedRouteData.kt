package com.mapbox.navigation.base.internal.route.parsing.models

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.DirectionsWaypoint
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.route.ResponseOriginAPI
import com.mapbox.navigation.base.route.RouterOrigin

@OptIn(ExperimentalMapboxNavigationAPI::class)
internal data class DirectionsParsedRouteData(
    val route: DirectionsRoute,
    val routesWaypoint: List<DirectionsWaypoint>?,
    val requestUUID: String?,
    val routeOptions: RouteOptions,
    val routeIndex: Int,
    @RouterOrigin val routerOrigin: String,
    @ResponseOriginAPI val responseOriginAPI: String,
)
