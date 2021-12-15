package com.mapbox.navigation.core.infra.factories

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point

fun createDirectionsRoute(
    legs: List<RouteLeg> = listOf(createRouteLeg()), // each route should have at least one leg,
    routeOptions: RouteOptions = createRouteOptions()
): DirectionsRoute {
    return DirectionsRoute.builder()
        .distance(5.0)
        .duration(9.0)
        .legs(legs)
        .routeOptions(routeOptions)
        .build()
}

fun createRouteLeg(): RouteLeg {
    return RouteLeg.builder().build()
}

fun createRouteOptions(
    // the majority of tests needs 2 waypoints
    coordinatesList: List<Point> = listOf(
        Point.fromLngLat(1.0, 1.0),
        Point.fromLngLat(2.0, 2.0),
    ),
    profile: String = DirectionsCriteria.PROFILE_DRIVING
): RouteOptions {
    return RouteOptions
        .builder()
        .coordinatesList(coordinatesList)
        .profile(profile)
        .build()
}
