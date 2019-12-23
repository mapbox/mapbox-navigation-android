package com.mapbox.services.android.navigation.v5.internal.navigation

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.services.android.navigation.v5.navigation.DirectionsRouteType
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation
import com.mapbox.services.android.navigation.v5.navigation.RefreshCallback
import com.mapbox.services.android.navigation.v5.navigation.RefreshError
import java.util.Date
import timber.log.Timber

internal class RouteRefresherCallback(
    private val mapboxNavigation: MapboxNavigation,
    private val routeRefresher: RouteRefresher
) : RefreshCallback {

    override fun onRefresh(directionsRoute: DirectionsRoute) {
        mapboxNavigation.startNavigation(directionsRoute, DirectionsRouteType.FRESH_ROUTE)
        routeRefresher.updateLastRefresh(Date())
        routeRefresher.updateIsChecking(false)
    }

    override fun onError(error: RefreshError) {
        error.message?.let {
            Timber.w(it)
        }
        routeRefresher.updateIsChecking(false)
    }
}
