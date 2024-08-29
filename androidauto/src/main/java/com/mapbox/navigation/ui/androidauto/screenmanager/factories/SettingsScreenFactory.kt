package com.mapbox.navigation.ui.androidauto.screenmanager.factories

import androidx.car.app.CarContext
import androidx.car.app.Screen
import com.mapbox.navigation.ui.androidauto.MapboxCarContext
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreenFactory
import com.mapbox.navigation.ui.androidauto.settings.CarSettingsScreen

class SettingsScreenFactory(
    private val mapboxCarContext: MapboxCarContext,
) : MapboxScreenFactory {
    override fun create(carContext: CarContext): Screen {
        return CarSettingsScreen(mapboxCarContext)
    }
}
