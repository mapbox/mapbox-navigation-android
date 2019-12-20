@file:JvmName("Mappers")

package com.mapbox.navigation.examples.utils.extensions

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.navigation.base.route.model.Route

fun Route.mapToDirectionsRoute(): DirectionsRoute {
    val duration = duration.toDouble()
    val legs = legs?.legs?.let { it as List<RouteLeg> }

    return DirectionsRoute.builder()
        .distance(distance)
        .duration(duration)
        .geometry(geometry)
        .weight(weight)
        .weightName(weightName)
        .voiceLanguage(voiceLanguage)
        .legs(legs)
        .build()
}