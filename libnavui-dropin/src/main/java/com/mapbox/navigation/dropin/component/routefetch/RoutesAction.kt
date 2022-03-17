package com.mapbox.navigation.dropin.component.routefetch

import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin

sealed class RoutesAction {
    data class FetchPoints(val points: List<Point>) : RoutesAction()
    data class FetchOptions(val options: RouteOptions) : RoutesAction()
    data class SetRoutes(val routes: List<NavigationRoute>) : RoutesAction()
    data class Ready(val routes: List<NavigationRoute>) : RoutesAction()
    data class Failed(
        val reasons: List<RouterFailure>,
        val routeOptions: RouteOptions
    ) : RoutesAction()
    data class Canceled(
        val routeOptions: RouteOptions,
        val routerOrigin: RouterOrigin
    ) : RoutesAction()
}
