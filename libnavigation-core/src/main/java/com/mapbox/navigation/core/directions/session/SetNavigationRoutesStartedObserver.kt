package com.mapbox.navigation.core.directions.session

import com.mapbox.navigation.base.route.NavigationRoute

internal fun interface SetNavigationRoutesStartedObserver {
    fun onRoutesSetStarted(params: RoutesSetStartedParams)
}

internal data class RoutesSetStartedParams(
    val routes: List<NavigationRoute>
)
