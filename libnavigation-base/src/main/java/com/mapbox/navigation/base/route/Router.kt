package com.mapbox.navigation.base.route

import android.location.Location
import com.mapbox.navigation.base.route.model.Route

interface Router {

    fun getRoute(
        origin: Location,
        waypoints: List<Location>?,
        destination: Location,
        callback: RouteCallback
    )

    fun cancel()

    interface RouteCallback {
        fun onRouteReady(routes: List<Route>)

        fun onFailure(throwable: Throwable)
    }
}
