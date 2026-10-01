package com.mapbox.navigation.ui.androidauto.screenmanager.factories

import androidx.car.app.CarContext
import androidx.car.app.Screen
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.ui.androidauto.MapboxCarContext
import com.mapbox.navigation.ui.androidauto.freedrive.FreeDriveCarScreen
import com.mapbox.navigation.ui.androidauto.internal.logAndroidAutoFailure
import com.mapbox.navigation.ui.androidauto.preview.CarRoutePreviewScreen2
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreen
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreenFactory

/**
 * Default screen for [MapboxScreen.ROUTE_PREVIEW].
 *
 * @deprecated Use [MapboxNavigationScreenFactory] for [MapboxScreen.NAVIGATION] instead.
 */
@ExperimentalPreviewMapboxNavigationAPI
@Deprecated("Use MapboxNavigationScreenFactory for MapboxScreen.NAVIGATION instead.")
class RoutePreviewScreenFactory2(
    private val mapboxCarContext: MapboxCarContext,
) : MapboxScreenFactory {

    override fun create(carContext: CarContext): Screen {
        val repository = mapboxCarContext.routePreviewRequest.repository
        val routes = repository?.routes?.value
        if (!routes.isNullOrEmpty()) {
            val mapboxNavigation = MapboxNavigationApp.current()
            if (mapboxNavigation == null) {
                // Without navigation the routes can't be previewed, and the preview screen would
                // stay in its loading state. Show free drive instead, as RoutePreviewScreenFactory
                // does when there is nothing to preview.
                logAndroidAutoFailure(
                    "RoutePreviewScreenFactory2 showing free drive, MapboxNavigation is detached",
                )
                return FreeDriveCarScreen(mapboxCarContext)
            }
            mapboxNavigation.setRoutesPreview(routes)
        }
        return CarRoutePreviewScreen2(mapboxCarContext)
    }
}
