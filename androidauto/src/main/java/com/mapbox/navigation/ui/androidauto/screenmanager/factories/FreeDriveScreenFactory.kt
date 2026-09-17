package com.mapbox.navigation.ui.androidauto.screenmanager.factories

import androidx.car.app.CarContext
import androidx.car.app.Screen
import com.mapbox.navigation.ui.androidauto.MapboxCarContext
import com.mapbox.navigation.ui.androidauto.freedrive.FreeDriveCarScreen
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreen
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreenFactory

/**
 * Default screen for [MapboxScreen.FREE_DRIVE].
 *
 * @deprecated Use [MapboxNavigationScreenFactory] for [MapboxScreen.NAVIGATION] instead.
 */
@Deprecated("Use MapboxNavigationScreenFactory for MapboxScreen.NAVIGATION instead.")
class FreeDriveScreenFactory(
    private val mapboxCarContext: MapboxCarContext,
) : MapboxScreenFactory {
    override fun create(carContext: CarContext): Screen {
        return FreeDriveCarScreen(mapboxCarContext)
    }
}
