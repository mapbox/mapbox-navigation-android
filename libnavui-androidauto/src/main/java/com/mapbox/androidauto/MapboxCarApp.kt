package com.mapbox.androidauto

import androidx.annotation.UiThread
import com.mapbox.androidauto.navigation.location.CarAppLocation
import com.mapbox.androidauto.navigation.location.impl.CarAppLocationImpl
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp

/**
 * The entry point for your Mapbox Android Auto app.
 */
object MapboxCarApp {

    /**
     * Location service available to the car and app.
     */
    fun carAppLocationService(): CarAppLocation =
        MapboxNavigationApp.getObserver(CarAppLocation::class)

    /**
     * Setup android auto with defaults
     */
    @UiThread
    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    fun setup() {
        if (MapboxNavigationApp.getObservers(CarAppLocation::class).isEmpty()) {
            MapboxNavigationApp.registerObserver(CarAppLocationImpl())
        }
    }
}
