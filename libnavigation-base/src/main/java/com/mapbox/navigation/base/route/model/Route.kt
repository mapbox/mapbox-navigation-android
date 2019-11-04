package com.mapbox.navigation.base.route.model

import com.mapbox.api.directions.v5.models.RouteLeg

data class Route(
    val routeIndex: String?,
    val distance: Double?,
    val duration: Long?,
    val geometry: String?,
    val weight: Double?,
    val weightName: String?,
    val legs: List<RouteLeg>?,
    val voiceLanguage: String?
)
