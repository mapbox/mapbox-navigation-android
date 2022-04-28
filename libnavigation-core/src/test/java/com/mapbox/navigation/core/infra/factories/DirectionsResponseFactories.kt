package com.mapbox.navigation.core.infra.factories

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.route.NavigationRoute
import io.mockk.every
import io.mockk.mockk

fun createNavigationRoute(
    directionsRoute: DirectionsRoute = createDirectionsRoute()
): NavigationRoute {
    requireNotNull(directionsRoute.routeOptions())
    return mockk {
        every { this@mockk.routeOptions } returns directionsRoute.routeOptions()!!
        every { this@mockk.directionsRoute } returns directionsRoute
    }
}

fun createDirectionsRoute(
    legs: List<RouteLeg> = listOf(createRouteLeg()),
    routeOptions: RouteOptions = createRouteOptions(),
    distance: Double = 5.0,
    duration: Double = 9.0,
    routeIndex: String = "0"
): DirectionsRoute = DirectionsRoute.builder()
    .distance(distance)
    .duration(duration)
    .legs(legs)
    .routeOptions(routeOptions)
    .routeIndex(routeIndex)
    .build()

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
