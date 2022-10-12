package com.mapbox.androidauto.screenmanager.factories

import androidx.car.app.CarContext
import androidx.car.app.Screen
import com.mapbox.androidauto.car.MapboxCarContext
import com.mapbox.androidauto.car.settings.CarSettingsScreen
import com.mapbox.androidauto.screenmanager.MapboxScreenFactory

class SettingsScreenFactory(
    private val mapboxCarContext: MapboxCarContext
) : MapboxScreenFactory {
    override fun create(carContext: CarContext): Screen {
        return CarSettingsScreen(mapboxCarContext)
    }
}
