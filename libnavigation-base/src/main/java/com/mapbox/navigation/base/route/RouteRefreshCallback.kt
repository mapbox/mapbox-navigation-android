package com.mapbox.navigation.base.route

import com.mapbox.api.directions.v5.models.DirectionsRoute

interface RouteRefreshCallback {
    fun onRefresh(directionsRoute: DirectionsRoute)

    fun onError(error: RouteRefreshError)
}

data class RouteRefreshError(
    val message: String? = null,
    val throwable: Throwable? = null
)
