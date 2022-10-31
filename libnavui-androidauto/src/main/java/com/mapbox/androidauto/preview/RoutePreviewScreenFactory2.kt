package com.mapbox.androidauto.preview

import androidx.car.app.CarContext
import androidx.car.app.Screen
import com.mapbox.androidauto.MapboxCarContext
import com.mapbox.androidauto.freedrive.FreeDriveCarScreen
import com.mapbox.androidauto.internal.logAndroidAuto
import com.mapbox.androidauto.screenmanager.MapboxScreenFactory
import com.mapbox.androidauto.screenmanager.factories.RoutePreviewScreenFactory
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp

/**
 * This will replace [RoutePreviewScreenFactory]
 */
@ExperimentalPreviewMapboxNavigationAPI
class RoutePreviewScreenFactory2(
    private val mapboxCarContext: MapboxCarContext
) : MapboxScreenFactory {
    override fun create(carContext: CarContext): Screen {
        val repository = mapboxCarContext.routePreviewRequest.repository
        val placeRecord = repository?.placeRecord?.value
        val routes = repository?.routes?.value
            ?: MapboxNavigationApp.current()?.getRoutesPreview()?.originalRoutesList
            ?: emptyList()
        return if (placeRecord == null || routes.isEmpty()) {
            logAndroidAuto(
                "Showing free drive screen because route preview can only be shown " +
                    "when there is a route. placeRecord=$placeRecord routes.size=${routes.size}"
            )
            FreeDriveCarScreen(mapboxCarContext)
        } else {
            MapboxNavigationApp.current()!!.setRoutesPreview(routes)
            CarRoutePreviewScreen2(mapboxCarContext, placeRecord)
        }
    }
}
