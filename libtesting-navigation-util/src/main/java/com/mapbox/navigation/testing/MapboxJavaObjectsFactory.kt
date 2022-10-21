package com.mapbox.navigation.testing

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point

object MapboxJavaObjectsFactory {

    fun routeOptions(
        profile: String = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC,
        coordinates: List<Point> = listOf(Point.fromLngLat(1.0, 50.0), Point.fromLngLat(34.9, 87.9))
    ): RouteOptions = RouteOptions
        .builder()
        .profile(profile)
        .coordinatesList(coordinates)
        .build()

    fun directionsRoute(
        routeOptions: RouteOptions? = null
    ): DirectionsRoute = DirectionsRoute.builder()
        .distance(34.5)
        .duration(12.3)
        .routeOptions(routeOptions)
        .build()
}
