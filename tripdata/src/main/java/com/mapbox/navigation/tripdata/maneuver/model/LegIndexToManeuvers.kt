package com.mapbox.navigation.tripdata.maneuver.model

internal data class LegIndexToManeuvers(
    val legIndex: Int,
    val stepIndexToManeuvers: List<StepIndexToManeuvers>,
)
