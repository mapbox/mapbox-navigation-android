package com.mapbox.navigation.ui.androidauto.screenmanager

import androidx.car.app.CarContext
import androidx.car.app.Screen

/**
 * Define your car screen factory. This gives you the ability to customize the navigation app
 * you are building with Mapbox.
 */
fun interface MapboxScreenFactory {
    fun create(carContext: CarContext): Screen
}
