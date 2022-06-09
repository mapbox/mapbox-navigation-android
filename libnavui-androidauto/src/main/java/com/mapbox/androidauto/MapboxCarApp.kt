package com.mapbox.androidauto

import android.app.Application
import com.mapbox.androidauto.navigation.location.CarAppLocation
import com.mapbox.androidauto.navigation.location.impl.CarAppLocationImpl
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.ui.app.internal.SharedApp
import com.mapbox.navigation.ui.voice.internal.MapboxAudioGuidance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * The entry point for your Mapbox Android Auto app.
 */
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
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

    /**
     * Setup android auto from your [Application.onCreate]
     *
     * @param application used to detect when activities are foregrounded
     */
    fun setup(application: Application) {
        SharedApp.setup(application)
        MapboxNavigationApp.registerObserver(CarAppLocationImpl())
    }
}
