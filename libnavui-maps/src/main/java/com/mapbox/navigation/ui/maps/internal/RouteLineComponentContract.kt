package com.mapbox.navigation.ui.maps.internal

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.ui.maps.internal.ui.RouteLineComponent
import kotlinx.coroutines.flow.StateFlow

/**
 * This is a communication contract for the [RouteLineComponent].
 */
@ExperimentalPreviewMapboxNavigationAPI
interface RouteLineComponentContract {
    val navigationRoutes: StateFlow<List<NavigationRoute>>
    fun setRoutes(routes: List<NavigationRoute>)
}
