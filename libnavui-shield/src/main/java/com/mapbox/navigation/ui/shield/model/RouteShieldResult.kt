package com.mapbox.navigation.ui.shield.model

import com.mapbox.bindgen.Expected

data class RouteShieldResult(
    val shields: Expected<List<RouteShieldError>, List<RouteShield>>
)
