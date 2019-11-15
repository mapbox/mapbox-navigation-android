package com.mapbox.navigation.route.offboard.extension

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.route.model.Route
import com.mapbox.navigation.base.route.model.RouteLegsNavigation

fun DirectionsRoute.mapToRoute() = Route(
    routeIndex = routeIndex(),
    distance = distance(),
    duration = duration()?.toLong(),
    geometry = geometry(),
    weight = weight(),
    weightName = weightName(),
    voiceLanguage = voiceLanguage(),
    legs = legs()?.let { RouteLegsNavigation(it) }
)
