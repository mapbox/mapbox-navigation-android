package com.mapbox.navigation.base.route

import com.mapbox.navigation.base.route.model.PointNavigation
import com.mapbox.navigation.base.route.model.Route

interface Router {

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
