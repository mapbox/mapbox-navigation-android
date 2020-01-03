package com.mapbox.navigation.base.route.model

data class StepManeuverNavigation constructor(
    @StepManeuverType
    var type: String? = null,
    var modifier: String? = null
)