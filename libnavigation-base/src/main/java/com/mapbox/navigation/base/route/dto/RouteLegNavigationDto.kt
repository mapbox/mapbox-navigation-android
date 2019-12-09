package com.mapbox.navigation.base.route.dto

import com.mapbox.navigation.base.route.model.RouteLegNavigation

class RouteLegNavigationDto(
    val distance: Double?,
    val duration: Double?,
    val summary: String?
)

fun RouteLegNavigationDto.mapToModel() = RouteLegNavigation(
    distance = distance,
    duration = duration,
    summary = summary
)