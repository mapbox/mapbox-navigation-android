package com.mapbox.navigation.core.internal

data class RoutesProgressData(
    val primary: RouteProgressData,
    val alternatives: Map<String, RouteProgressData>,
)
