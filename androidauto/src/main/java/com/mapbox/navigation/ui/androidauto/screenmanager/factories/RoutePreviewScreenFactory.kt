package com.mapbox.navigation.ui.androidauto.screenmanager.factories

import androidx.car.app.CarContext
import androidx.car.app.Screen
import com.mapbox.navigation.ui.androidauto.MapboxCarContext
import com.mapbox.navigation.ui.androidauto.freedrive.FreeDriveCarScreen
import com.mapbox.navigation.ui.androidauto.internal.logAndroidAuto
import com.mapbox.navigation.ui.androidauto.preview.CarRoutePreviewScreen
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreen
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreenFactory

/**
 * Default screen for [MapboxScreen.ROUTE_PREVIEW].
 */
class RoutePreviewScreenFactory(
    private val mapboxCarContext: MapboxCarContext,
) : MapboxScreenFactory {
    override fun create(carContext: CarContext): Screen {
        val repository = mapboxCarContext.routePreviewRequest.repository
        val placeRecord = repository?.placeRecord?.value
        val routes = repository?.routes?.value ?: emptyList()
        return if (placeRecord == null || routes.isEmpty()) {
            logAndroidAuto(
                "Showing free drive screen because route preview can only be shown " +
                    "when there is a route. placeRecord=$placeRecord routes.size=${routes.size}",
            )
            FreeDriveCarScreen(mapboxCarContext)
        } else {
            CarRoutePreviewScreen(mapboxCarContext, placeRecord, routes)
        }
    }
}
