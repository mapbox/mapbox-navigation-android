package com.mapbox.navigation.ui.androidauto.screenmanager.factories

import androidx.car.app.CarContext
import androidx.car.app.Screen
import com.mapbox.navigation.ui.androidauto.permissions.NeedsLocationPermissionsScreen
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreen
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreenFactory

/**
 * Default screen for [MapboxScreen.NEEDS_LOCATION_PERMISSION].
 */
class NeedsLocationPermissionScreenFactory : MapboxScreenFactory {
    override fun create(carContext: CarContext): Screen {
        return NeedsLocationPermissionsScreen(carContext)
    }
}
