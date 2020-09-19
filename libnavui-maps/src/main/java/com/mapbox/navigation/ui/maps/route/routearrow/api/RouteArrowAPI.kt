package com.mapbox.navigation.ui.maps.route.routearrow.api

import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.ui.base.MapboxView
import com.mapbox.navigation.ui.maps.route.routearrow.model.RouteArrowState

interface RouteArrowAPI {
    fun hideManeuverArrow()
    fun showManeuverArrow()
    fun redrawArrow()
    fun getRouteArrowVisibility(style: Style): Visibility?
    fun addUpComingManeuverArrow(routeProgress: RouteProgress)
    fun updateViewStyle(style: Style)
}
