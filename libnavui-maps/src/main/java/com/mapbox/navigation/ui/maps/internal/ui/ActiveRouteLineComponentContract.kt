package com.mapbox.navigation.ui.maps.internal.ui

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.MapboxNavigation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class ActiveRouteLineComponentContract : RouteLineComponentContract {
    override fun setRoutes(mapboxNavigation: MapboxNavigation, routes: List<NavigationRoute>) {
        mapboxNavigation.setNavigationRoutes(routes)
    }

    override fun getRouteInPreview(): Flow<List<NavigationRoute>?> {
        return flowOf(null)
    }
}
