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
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp

/**
 * Default screen for [MapboxScreen.ROUTE_PREVIEW].
 */
class RoutePreviewScreenFactory(
    private val mapboxCarContext: MapboxCarContext
) : MapboxScreenFactory {
    override fun create(carContext: CarContext): Screen {
        return CarRoutePreviewScreen(RoutePreviewCarContext(mapboxCarContext))
    }
}
