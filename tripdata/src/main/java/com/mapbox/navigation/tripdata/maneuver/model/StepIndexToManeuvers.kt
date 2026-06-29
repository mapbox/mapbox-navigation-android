package com.mapbox.navigation.tripdata.maneuver.model

internal data class StepIndexToManeuvers(
    val stepIndex: Int,
    val maneuverList: MutableList<Maneuver>,
)
