package com.mapbox.navigation.dropin.component.routefetch

import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.route.NavigationRoute

sealed class RoutesAction {
    object FetchAndSetRoute : RoutesAction()
    object StartNavigation : RoutesAction()
    object StopNavigation : RoutesAction()

    data class FetchPoints(val points: List<Point>) : RoutesAction()
    data class FetchOptions(val options: RouteOptions) : RoutesAction()
    data class SetRoutes(val routes: List<NavigationRoute>, val legIndex: Int = 0) : RoutesAction()

    object DidStartNavigation : RoutesAction()
    object DidStopNavigation : RoutesAction()
}
