package com.mapbox.navigation.ui.shield.model

data class RouteShieldResult(
    val shields: List<RouteShield>,
    val errors: List<RouteShield>
)
