package com.mapbox.androidauto

import com.mapbox.androidauto.navigation.location.CarAppLocation
import com.mapbox.androidauto.navigation.location.impl.CarAppLocationImpl
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.ui.app.internal.SharedApp
import com.mapbox.navigation.ui.voice.internal.MapboxAudioGuidance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * The entry point for your Mapbox Android Auto app.
 */
object MapboxCarApp : MapboxNavigationObserver {
    private val carAppStateFlow = MutableStateFlow<CarAppState>(FreeDriveState)
    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    private val carAppLocation: CarAppLocation = CarAppLocationImpl()

    /**
     * Attach observers to the CarAppState to determine which view to show.
     */
    val carAppState: StateFlow<CarAppState> = carAppStateFlow

    /**
     * Location service available to the car and app.
     */
    fun carAppLocationService(): CarAppLocation = carAppLocation

    /**
     * Audio guidance service available to the car and app.
     */
    fun carAppAudioGuidanceService(): MapboxAudioGuidance =
        MapboxNavigationApp.getObserver(MapboxAudioGuidance::class)

    /**
     * Keep your car and app in sync with CarAppState.
     */
    fun updateCarAppState(carAppState: CarAppState) {
        carAppStateFlow.value = carAppState
    }

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        // TODO add after 2.8.0-rc.1
//        MapboxNavigationApp.registerObserver(SharedApp)
        MapboxNavigationApp.registerObserver(carAppLocation)
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        MapboxNavigationApp.unregisterObserver(carAppLocation)
    }
}
