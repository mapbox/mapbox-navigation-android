package com.mapbox.navigation.ui.app.internal

import android.content.Context
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.internal.extensions.attachCreated
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.ui.app.internal.controller.AudioGuidanceStateController
import com.mapbox.navigation.ui.app.internal.controller.CameraStateController
import com.mapbox.navigation.ui.app.internal.controller.DestinationStateController
import com.mapbox.navigation.ui.app.internal.controller.LocationStateController
import com.mapbox.navigation.ui.app.internal.controller.NavigationStateController
import com.mapbox.navigation.ui.app.internal.controller.RoutePreviewStateController
import com.mapbox.navigation.ui.app.internal.controller.RouteStateController
import com.mapbox.navigation.ui.app.internal.controller.StateResetController
import com.mapbox.navigation.ui.app.internal.controller.TripSessionStarterStateController
import com.mapbox.navigation.ui.utils.internal.datastore.NavigationDataStoreOwner
import com.mapbox.navigation.ui.voice.internal.MapboxAudioGuidance
import com.mapbox.navigation.ui.voice.internal.impl.MapboxAudioGuidanceImpl

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
object SharedApp {
    private var isSetup = false

    val store = Store()
    val state get() = store.state.value

    /**
     * These classes are accessible through MapboxNavigationApp.getObserver(..)
     */
    val navigationStateController = NavigationStateController(store)
    val locationStateController = LocationStateController(store)
    val tripSessionStarterStateController = TripSessionStarterStateController(store)
    val audioGuidanceStateController = AudioGuidanceStateController(store)
    val cameraStateController = CameraStateController(store)
    val destinationStateController = DestinationStateController(store)
    val routeStateController = RouteStateController(store)
    val routePreviewStateController = RoutePreviewStateController(store)
    private val navigationObservers: Array<MapboxNavigationObserver> = arrayOf(
        destinationStateController,
        tripSessionStarterStateController,
        audioGuidanceStateController,
        locationStateController,
        routeStateController,
        routePreviewStateController,
        cameraStateController,
        navigationStateController,
    )

    fun setup(
        context: Context,
        audioGuidance: MapboxAudioGuidance? = null
    ) {
        if (isSetup) return
        isSetup = true

        MapboxNavigationApp.registerObserver(StateResetController(store))
        MapboxNavigationApp.lifecycleOwner.attachCreated(*navigationObservers)
        MapboxNavigationApp.registerObserver(audioGuidance ?: defaultAudioGuidance(context))
    }

    private fun defaultAudioGuidance(context: Context): MapboxAudioGuidance {
        return MapboxAudioGuidanceImpl.create(context).also {
            it.dataStoreOwner = NavigationDataStoreOwner(context, DEFAULT_DATA_STORE_NAME)
        }
    }

    private const val DEFAULT_DATA_STORE_NAME = "mapbox_navigation_preferences"
}
