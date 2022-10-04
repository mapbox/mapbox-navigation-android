package com.mapbox.navigation.core.fasterroute

import com.mapbox.navigation.base.route.NavigationRoute

fun interface NewFasterRouteObserver {
    fun onNewFasterRouteFound(newFasterRoute: NewFasterRoute)
}

class NewFasterRoute(
    val fasterRoute: NavigationRoute,
    val fasterThanPrimary: Double
)