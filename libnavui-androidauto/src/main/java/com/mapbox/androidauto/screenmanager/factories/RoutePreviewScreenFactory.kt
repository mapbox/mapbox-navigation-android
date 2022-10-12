package com.mapbox.androidauto.screenmanager.factories

import androidx.car.app.CarContext
import androidx.car.app.Screen
import com.mapbox.androidauto.car.FreeDriveCarScreen
import com.mapbox.androidauto.car.MapboxCarContext
import com.mapbox.androidauto.car.preview.CarRoutePreviewScreen
import com.mapbox.androidauto.car.preview.RoutePreviewCarContext
import com.mapbox.androidauto.internal.logAndroidAuto
import com.mapbox.androidauto.screenmanager.MapboxScreen
import com.mapbox.androidauto.screenmanager.MapboxScreenFactory

/**
 * Default screen for [MapboxScreen.ROUTE_PREVIEW].
 */
class RoutePreviewScreenFactory(
    private val mapboxCarContext: MapboxCarContext
) : MapboxScreenFactory {
    override fun create(carContext: CarContext): Screen {
        val repository = mapboxCarContext.carRoutePreviewRequest.repository
        val placeRecord = repository?.placeRecord?.value
        val routes = repository?.routes?.value ?: emptyList()
        return if (placeRecord == null || routes.isEmpty()) {
            logAndroidAuto(
                "Showing free drive screen because route preview can only be shown " +
                    "when there is a route. placeRecord=$placeRecord routes.size=${routes.size}"
            )
            FreeDriveCarScreen(mapboxCarContext)
        } else {
            CarRoutePreviewScreen(RoutePreviewCarContext(mapboxCarContext), placeRecord, routes)
        }
    }
}
