package com.mapbox.navigation.ui.maps.route.line.model

internal data class RouteLineRestrictedSectionData(
    val offset: Double,
    val isInRestrictedSection: Boolean = false,
    val legIndex: Int = 0,
    val isLegOrigin: Boolean = false
)
