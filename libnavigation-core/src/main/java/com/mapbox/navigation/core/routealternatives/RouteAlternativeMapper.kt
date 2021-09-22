package com.mapbox.navigation.core.routealternatives

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.trip.model.RouteProgress


internal object RouteAlternativeMapper {
    fun from(
        routeAlternatives: List<com.mapbox.navigator.RouteAlternative>
    ): List<RouteAlternative> {
        return routeAlternatives.map { fromNativeRouteAlternative(it) }
    }

    fun fromNativeRouteAlternative(
        routeAlternative: com.mapbox.navigator.RouteAlternative
    ) = RouteAlternative(
        directionsRoute = DirectionsRoute.fromJson(routeAlternative.route),
        currentRouteFork = fromNativeRouteAlternative(routeAlternative.currentRouteFork),
        alternativeRouteFork = fromNativeRouteAlternative(routeAlternative.alternativeRouteFork)
    )

    private fun fromNativeRouteAlternative(
        currentRouteFork: com.mapbox.navigator.RouteIntersection
    ) = RouteIntersection(
        point = currentRouteFork.location,
        segmentIndex = currentRouteFork.segmentIndex,
        legIndex = currentRouteFork.legIndex
    )
}
