package com.mapbox.navigation.ui.maneuver.model

internal data class RoadShieldResult(
    val shields: Map<String, RoadShield?>,
    val errors: Map<String, RoadShieldError>
)
