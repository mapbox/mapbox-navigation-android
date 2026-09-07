package com.mapbox.navigation.base.internal.route.parsing.parser

import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.DirectionsWaypoint
import com.mapbox.api.directions.v5.models.RouteOptions

internal fun DirectionsResponse.getDirectionsRoute(
    routeIndex: Int,
    routeOptions: RouteOptions,
    // Overrides the routeIndex field of the returned DirectionsRoute.
    routeIndexOverride: Int = routeIndex,
): DirectionsRoute {
    return this.routes()[routeIndex].toBuilder()
        .requestUuid(this.uuid())
        .routeIndex(routeIndexOverride.toString())
        .routeOptions(routeOptions)
        .build()
}

internal fun DirectionsResponse.getDirectionsWaypoint(routeIndex: Int): List<DirectionsWaypoint>? {
    return this.routes()[routeIndex].waypoints() ?: this.waypoints()
}
