package com.mapbox.navigation.base.route.model

data class RouteLegsNavigation constructor(
    var distance: Double? = null,
    var duration: Double? = null,
    var summary: String? = null,
    var steps: List<LegStepNavigation>? = null
)
