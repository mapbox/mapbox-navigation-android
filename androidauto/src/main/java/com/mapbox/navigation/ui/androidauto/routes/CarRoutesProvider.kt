package com.mapbox.navigation.ui.androidauto.routes

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.ui.androidauto.navigation.CarNavigationCamera
import com.mapbox.navigation.ui.androidauto.preview.CarRouteLineRenderer
import kotlinx.coroutines.flow.Flow

/**
 * Gives you the ability to provide routes to different car route map components.
 *
 * @see [CarRouteLineRenderer]
 * @see [CarNavigationCamera]
 */
interface CarRoutesProvider {
    /**
     * Provides navigation routes for active guidance or route preview.
     */
    val navigationRoutes: Flow<List<NavigationRoute>>
}
