package com.mapbox.navigation.ui.maps.route.routearrow.api

import com.mapbox.maps.Style
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.ui.maps.route.routearrow.model.RouteArrowState

interface RouteArrowActions {
    fun hideRouteArrowState(): RouteArrowState.UpdateRouteArrowVisibilityState
    fun showRouteArrowState(): RouteArrowState.UpdateRouteArrowVisibilityState
    fun getAddUpcomingManeuverArrowState(
        routeProgress: RouteProgress
    ): RouteArrowState.UpdateManeuverArrowState

    fun redraw(): RouteArrowState.UpdateManeuverArrowState
    fun getUpdateViewStyleState(style: Style): RouteArrowState.UpdateViewStyleState
}
