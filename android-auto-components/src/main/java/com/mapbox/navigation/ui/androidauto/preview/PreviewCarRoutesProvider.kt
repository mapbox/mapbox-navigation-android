package com.mapbox.navigation.ui.androidauto.preview

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.ui.androidauto.navigation.CarNavigationCamera
import com.mapbox.navigation.ui.androidauto.routes.CarRoutesProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Gives the [CarRoutePreviewScreen] the ability to control the selected route rendered
 * by the [CarRouteLineRenderer] and [CarNavigationCamera].
 */
class PreviewCarRoutesProvider(routes: List<NavigationRoute>) : CarRoutesProvider {

    private val _routes = MutableStateFlow(routes)
    override val navigationRoutes: StateFlow<List<NavigationRoute>> = _routes

    /**
     * When the route selection changes, update the route selection. This will relay the route
     * selection to other components.
     */
    fun updateRoutes(routes: List<NavigationRoute>) {
        _routes.value = routes
    }
}
