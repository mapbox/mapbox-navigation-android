package com.mapbox.navigation.base.route.dto

import com.mapbox.navigation.base.route.model.RouteLegNavigation

class RouteLegNavigationDto(
    val distance: Double?,
    val duration: Double?,
    val summary: String?,
    val steps: List<LegStepNavigationDto>?,
    val annotation: LegAnnotationNavigationDto?
)

fun RouteLegNavigationDto.mapToRouteLegNavigation(): RouteLegNavigation =
    RouteLegNavigation.Builder()
        .distance(distance)
        .duration(duration)
        .summary(summary)
        .steps(steps?.map { it.mapToModel() })
        .build()
