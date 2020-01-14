package com.mapbox.navigation.base.route

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions


interface Router {

    fun getRoute(
        routeOptions: RouteOptions,
        callback: Callback
    )

    fun cancel()

    interface Callback {
        fun onResponse(routes: List<DirectionsRoute>)

        fun onFailure(throwable: Throwable)
    }
}
