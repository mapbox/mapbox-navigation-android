package com.mapbox.navigation.core.fasterroute

import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute

@ExperimentalMapboxNavigationAPI
fun interface NewFasterRouteObserver {
    /***
     * Called every time a faster route is available.
     *
     * You can accept it using [FasterRoutes.acceptFasterRoute]
     * or decline it using [FasterRoutes.declineFasterRoute]
     */
    fun onNewFasterRouteFound(newFasterRoute: NewFasterRoute)
}

@ExperimentalMapboxNavigationAPI
class NewFasterRoute(
    val fasterRoute: NavigationRoute,
    val fasterThanPrimary: Double,
    val alternativeId: Int
)
