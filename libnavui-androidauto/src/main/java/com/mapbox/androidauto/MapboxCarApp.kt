package com.mapbox.androidauto

import androidx.annotation.UiThread
import com.mapbox.androidauto.navigation.location.CarAppLocation
import com.mapbox.androidauto.navigation.location.impl.CarAppLocationImpl
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * The entry point for your Mapbox Android Auto app.
 */
object MapboxCarApp {

    private val carAppStateFlow = MutableStateFlow<CarAppState>(FreeDriveState)

    /**
     * Attach observers to the CarAppState to determine which view to show.
     */
    val carAppState: StateFlow<CarAppState> = carAppStateFlow

    /**
     * Location service available to the car and app.
     */
    fun carAppLocationService(): CarAppLocation =
        MapboxNavigationApp.getObserver(CarAppLocation::class)


    /**
     * Keep your car and app in sync with CarAppState.
     */
    fun updateCarAppState(carAppState: CarAppState) {
        carAppStateFlow.value = carAppState
    }

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
