package com.mapbox.androidauto.car.routes

import com.mapbox.androidauto.car.navigation.CarNavigationCamera
import com.mapbox.androidauto.car.preview.CarRouteLine
import com.mapbox.navigation.base.route.NavigationRoute
import kotlinx.coroutines.flow.Flow

/**
 * Gives you the ability to provide routes to different car route map components.
 *
 * @see [CarRouteLine]
 * @see [CarNavigationCamera]
 */
interface CarRoutesProvider {
    /**
     * Provides navigation routes for active guidance or route preview.
     */
    val navigationRoutes: Flow<List<NavigationRoute>>
}
