package com.mapbox.navigation.ui.maps.route.arrow.api

import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.navigation.base.trip.model.RouteProgress

/**
 *
 */
interface RouteArrowAPI {

    /**
     *
     */
    fun hideManeuverArrow()

    /**
     *
     */
    fun showManeuverArrow()

    /**
     *
     */
    fun redrawArrow()

    /**
     *
     * @param style
     */
    fun getRouteArrowVisibility(style: Style): Visibility?

    /**
     *
     * @param routeProgress
     */
    fun addUpComingManeuverArrow(routeProgress: RouteProgress)

    /**
     *
     * @param style
     */
    fun updateViewStyle(style: Style)
}
