package com.mapbox.navigation.base.route.model

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions

data class Route(
    val routeIndex: String?,
    val distance: Double?,
    val duration: Long?,
    val geometry: String?,
    val weight: Double?,
    val weightName: String?,
    val legs: List<RouteLegNavigation>?,
    val routeOptions: RouteOptions?,
    val voiceLanguage: String?
)

fun DirectionsRoute.mapToRoute() = Route(
    routeIndex = routeIndex(),
    distance = distance(),
    duration = duration()?.toLong(),
    geometry = geometry(),
    weight = weight(),
    weightName = weightName(),
    legs = legs()?.map { it.mapToLeg() },
    routeOptions = routeOptions(),
    voiceLanguage = voiceLanguage()
)
