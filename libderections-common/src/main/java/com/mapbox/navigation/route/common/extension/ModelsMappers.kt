package com.mapbox.navigation.route.common.extension

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.route.model.Route

fun DirectionsRoute.mapToRoute() = Route(
    routeIndex = routeIndex(),
    distance = distance(),
    duration = duration()?.toLong(),
    geometry = geometry(),
    weight = weight(),
    weightName = weightName(),
    voiceLanguage = voiceLanguage(),
    legs = legs()
)
