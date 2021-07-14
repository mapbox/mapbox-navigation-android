package com.mapbox.navigation.ui.maneuver.model

internal data class LegIndexToManeuvers(
    val legIndex: Int,
    val stepIndexToManeuvers: List<StepIndexToManeuvers>
)
