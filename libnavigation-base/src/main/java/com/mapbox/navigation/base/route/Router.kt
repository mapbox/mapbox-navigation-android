package com.mapbox.navigation.base.route

import com.mapbox.navigation.base.route.model.Route
import com.mapbox.navigation.base.route.model.RouteOptionsNavigation

interface Router {

    fun getRoute(
        routeOptions: RouteOptionsNavigation,
        callback: Callback
    )

    fun cancel()

    interface Callback {
        fun onResponse(routes: List<Route>)

        fun onFailure(throwable: Throwable)
    }
}
