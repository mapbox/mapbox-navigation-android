package com.mapbox.navigation.base.route

import com.mapbox.navigation.base.route.model.PointNavigation
import com.mapbox.navigation.base.route.model.Route

interface Router {

    @Deprecated("Will be removed in v1.0. Use [getRoute(Point, List<Point>, RouteListener)] instead")
    fun getRoute(origin: PointNavigation, waypoints: List<PointNavigation>, callback: (route: Route) -> Unit)

    fun getRoute(
        origin: PointNavigation,
        waypoints: List<PointNavigation>?,
        destination: PointNavigation,
        listener: RouteListener
    )

    fun cancel()

    interface RouteListener {
        fun onRouteReady(route: Route)

        fun onFailure(throwable: Throwable)
    }
}
