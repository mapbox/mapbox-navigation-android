package com.mapbox.navigation.base.internal.route.parsing.parser

import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.DirectionsWaypoint
import com.mapbox.api.directions.v5.models.RouteOptions

internal fun DirectionsResponse.getDirectionsRoute(
    routeIndex: Int,
    routeOptions: RouteOptions,
): DirectionsRoute {
    return this.routes()[routeIndex].toBuilder()
        .requestUuid(this.uuid())
        .routeIndex(routeIndex.toString())
        .routeOptions(routeOptions)
        .build()
}

internal fun DirectionsResponse.getDirectionsWaypoint(routeIndex: Int): List<DirectionsWaypoint>? {
    return this.routes()[routeIndex].waypoints() ?: this.waypoints()
}
