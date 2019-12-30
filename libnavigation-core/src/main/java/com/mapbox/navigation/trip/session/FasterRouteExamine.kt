package com.mapbox.navigation.trip.session

import com.mapbox.navigation.base.route.model.Route
import com.mapbox.navigation.base.trip.model.RouteProgress

interface FasterRouteExamine {

    fun isRouteFaster(oldRoute: Route, routeProgress: RouteProgress, newRoute: Route): Boolean

    class Impl : FasterRouteExamine {
        // TODO need logic here
        override fun isRouteFaster(
            oldRoute: Route,
            routeProgress: RouteProgress,
            newRoute: Route
        ): Boolean = true
    }
}
