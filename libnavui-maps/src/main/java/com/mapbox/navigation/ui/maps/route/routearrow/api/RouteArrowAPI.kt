package com.mapbox.navigation.ui.maps.route.routearrow.api

import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.navigation.base.trip.model.RouteProgress

interface RouteArrowAPI {
    fun hideManeuverArrow()
    fun showManeuverArrow()
    fun redrawArrow()
    fun getRouteArrowVisibility(style: Style): Visibility?
    fun addUpComingManeuverArrow(routeProgress: RouteProgress)
    fun updateViewStyle(style: Style)
}
