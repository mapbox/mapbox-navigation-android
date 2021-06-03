package com.mapbox.navigation.ui.maneuver.model

import com.mapbox.api.directions.v5.models.RouteLeg

internal data class LegToManeuvers(
    val routeLeg: RouteLeg,
    val stepIndexToManeuvers: List<StepIndexToManeuvers>
)
