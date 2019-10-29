package com.mapbox.navigation.base.route.model

import com.mapbox.api.directions.v5.models.RouteLeg

data class RouteLegNavigation(
    val distance: Double?,
    val duration: Double?,
    val summary: String?,
    val steps: List<LegStepNavigation>?
)

fun RouteLeg.mapToLeg() = RouteLegNavigation(
    distance = distance(),
    duration = duration(),
    summary = summary(),
    steps = steps()?.map { it.mapToStep() }
)
