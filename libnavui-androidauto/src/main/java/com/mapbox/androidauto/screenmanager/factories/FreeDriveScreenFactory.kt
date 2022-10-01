package com.mapbox.androidauto.screenmanager.factories

import androidx.car.app.CarContext
import androidx.car.app.Screen
import com.mapbox.androidauto.car.FreeDriveCarScreen
import com.mapbox.androidauto.car.MapboxCarContext
import com.mapbox.androidauto.screenmanager.MapboxScreen
import com.mapbox.androidauto.screenmanager.MapboxScreenFactory

/**
 * Default screen for [MapboxScreen.FREE_DRIVE].
 */
class FreeDriveScreenFactory(
    private val mapboxCarContext: MapboxCarContext
) : MapboxScreenFactory {
    override fun create(carContext: CarContext): Screen {
        return FreeDriveCarScreen(mapboxCarContext)
    }
}
