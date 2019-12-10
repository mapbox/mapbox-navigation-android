package com.mapbox.navigation.base.route.dto

import com.mapbox.navigation.base.route.model.RouteLegNavigation

internal class RouteLegNavigationDto(
    val distance: Double?,
    val duration: Double?,
    val summary: String?,
    val steps: List<LegStepNavigationDto>?,
    val annotation: LegAnnotationNavigationDto?
)

internal fun RouteLegNavigationDto.mapToModelRouteLeg() = RouteLegNavigation(
    distance = distance,
    duration = duration,
    summary = summary,
    steps = steps?.map { it.mapToModel() },
    annotation = annotation?.mapToModel()
)
