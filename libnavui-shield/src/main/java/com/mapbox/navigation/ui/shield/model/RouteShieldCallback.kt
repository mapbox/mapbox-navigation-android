package com.mapbox.navigation.ui.shield.model

fun interface RouteShieldCallback {

    fun onRoadShields(
        shields: List<RouteShield>,
        errors: List<RouteShield>
    )
}
