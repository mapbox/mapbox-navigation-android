package com.mapbox.navigation.base.route.model

data class RouteLegNavigation(
    val distance: Double?,
    val duration: Double?,
    val summary: String?,
    val steps: List<LegStepNavigation>?
)
