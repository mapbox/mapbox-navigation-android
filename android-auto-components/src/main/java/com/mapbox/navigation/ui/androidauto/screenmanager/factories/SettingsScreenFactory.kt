package com.mapbox.navigation.ui.androidauto.screenmanager.factories

import androidx.car.app.CarContext
import androidx.car.app.Screen
import com.mapbox.navigation.ui.androidauto.MapboxCarContext
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreen
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreenFactory
import com.mapbox.navigation.ui.androidauto.settings.CarSettingsScreen

/**
 * Default dedicated settings screen for [MapboxScreen.SETTINGS].
 *
 * @deprecated Use [MapboxNavigationScreenFactory] for [MapboxScreen.NAVIGATION] instead.
 * Settings are shown inline on the navigation screen from Car API 7.
 */
@Deprecated("Use MapboxNavigationScreenFactory for MapboxScreen.NAVIGATION with inline settings instead.")
class SettingsScreenFactory(
    private val mapboxCarContext: MapboxCarContext,
) : MapboxScreenFactory {
    override fun create(carContext: CarContext): Screen {
        return CarSettingsScreen(mapboxCarContext)
    }
}
