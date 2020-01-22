package com.mapbox.navigation.core.directions.session

import com.mapbox.api.directions.v5.models.DirectionsRoute

interface RouteObserver {
    fun onRoutesChanged(routes: List<DirectionsRoute>)

    fun onRoutesRequested()

    fun onRoutesRequestFailure(throwable: Throwable)
}
