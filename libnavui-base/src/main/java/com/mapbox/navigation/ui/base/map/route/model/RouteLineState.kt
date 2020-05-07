package com.mapbox.navigation.ui.base.map.route.model

import com.mapbox.geojson.Feature
import com.mapbox.navigation.ui.base.State

data class RouteLineState(
    val options: RouteLineOptions,
    val features: List<Feature>
) : State
