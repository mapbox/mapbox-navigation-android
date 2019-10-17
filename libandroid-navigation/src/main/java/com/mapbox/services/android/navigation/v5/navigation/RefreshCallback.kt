package com.mapbox.services.android.navigation.v5.navigation

import com.mapbox.api.directions.v5.models.DirectionsRoute

interface RefreshCallback {
    fun onRefresh(directionsRoute: DirectionsRoute)

    fun onError(error: RefreshError)
}
