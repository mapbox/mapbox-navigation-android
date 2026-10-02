package com.mapbox.navigation.ui.androidauto.screenmanager.factories

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.annotations.RequiresCarApi
import com.mapbox.navigation.ui.androidauto.MapboxCarContext
import com.mapbox.navigation.ui.androidauto.navigation.MapboxNavigationScreen
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreen
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreenFactory

/**
 * Default screen factory for [MapboxScreen.NAVIGATION].
 */
@RequiresCarApi(7)
class MapboxNavigationScreenFactory(
    private val mapboxCarContext: MapboxCarContext,
) : MapboxScreenFactory {
    override fun create(carContext: CarContext): Screen = MapboxNavigationScreen(mapboxCarContext)
}
