package com.mapbox.androidauto

import android.app.Application
import com.mapbox.androidauto.navigation.location.CarAppLocation
import com.mapbox.androidauto.navigation.location.impl.CarAppLocationImpl
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.ui.voice.internal.MapboxAudioGuidance
import com.mapbox.navigation.ui.voice.internal.impl.MapboxAudioGuidanceImpl
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
     */
    fun setup() {
        MapboxNavigationApp.registerObserver(sharedServices)
        MapboxNavigationApp.registerObserver(CarAppLocationImpl())
    }

    // TODO can be replaced with `SharedApp` once backwards compatibility is fixed
    //    https://github.com/mapbox/mapbox-navigation-android/pull/6303
    private val sharedServices = object : MapboxNavigationObserver {
        private lateinit var audioGuidance: MapboxAudioGuidance
        override fun onAttached(mapboxNavigation: MapboxNavigation) {
            val context = mapboxNavigation.navigationOptions.applicationContext
            audioGuidance = MapboxAudioGuidanceImpl.create(context)
            MapboxNavigationApp.registerObserver(audioGuidance)
        }

        override fun onDetached(mapboxNavigation: MapboxNavigation) {
            // Never really happens but is here to separate concerns.
            MapboxNavigationApp.unregisterObserver(audioGuidance)
        }
    }
}
