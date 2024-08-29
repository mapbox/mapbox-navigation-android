package com.mapbox.navigation.ui.androidauto.screenmanager.factories

import androidx.car.app.CarContext
import androidx.car.app.Screen
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.ui.androidauto.MapboxCarContext
import com.mapbox.navigation.ui.androidauto.preview.CarRoutePreviewScreen2
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreen
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreenFactory

/**
 * Default screen for [MapboxScreen.ROUTE_PREVIEW].
 */
@ExperimentalPreviewMapboxNavigationAPI
class RoutePreviewScreenFactory2(
    private val mapboxCarContext: MapboxCarContext,
) : MapboxScreenFactory {

    override fun create(carContext: CarContext): Screen {
        val repository = mapboxCarContext.routePreviewRequest.repository
        val routes = repository?.routes?.value
        if (!routes.isNullOrEmpty()) {
            MapboxNavigationApp.current()!!.setRoutesPreview(routes)
        }
        return CarRoutePreviewScreen2(mapboxCarContext)
    }
}
