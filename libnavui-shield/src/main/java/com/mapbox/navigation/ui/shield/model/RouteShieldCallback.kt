package com.mapbox.navigation.ui.shield.model

import com.mapbox.bindgen.Expected

fun interface RouteShieldCallback {

    fun onRoadShields(
        shields: List<Expected<RouteShieldError, RouteShieldResult>>
    )
}
