package com.mapbox.navigation.ui.maneuver.model

internal data class StepIndexToManeuvers(
    val stepIndex: Int,
    val maneuverList: List<Maneuver>
)
