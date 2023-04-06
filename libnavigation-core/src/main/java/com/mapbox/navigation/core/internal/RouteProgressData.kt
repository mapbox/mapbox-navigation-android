package com.mapbox.navigation.core.internal

data class RouteProgressData(
    val legIndex: Int,
    val routeGeometryIndex: Int,
    val legGeometryIndex: Int?,
)
