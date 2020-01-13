package com.mapbox.navigation.base.route.dto

import com.mapbox.navigation.base.route.model.Route

class RouteDto(
    val routeIndex: String?,
    val distance: Double,
    val duration: Long,
    val geometry: String?,
    val weight: Double?,
    val weightName: String?,
    val legs: List<RouteLegNavigationDto>?,
    val routeOptions: RouteOptionsNavigationDto?,
    val voiceLanguage: String?
)

fun RouteDto.mapToModelRoute() = Route(
    routeIndex = routeIndex,
    distance = distance,
    duration = duration,
    geometry = geometry,
    weight = weight,
    weightName = weightName,
    legs = legs?.map { it.mapToRouteLegNavigation() },
    routeOptions = routeOptions?.mapToModel(),
    voiceLanguage = voiceLanguage
)
