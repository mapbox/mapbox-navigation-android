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
import com.mapbox.navigation.ui.app.internal.controller.RouteAlternativeStateController
import com.mapbox.navigation.ui.app.internal.controller.RoutePreviewStateController
import com.mapbox.navigation.ui.app.internal.controller.RouteStateController
import com.mapbox.navigation.ui.app.internal.controller.StateResetController
import com.mapbox.navigation.ui.app.internal.controller.TripSessionStarterStateController
import com.mapbox.navigation.ui.utils.internal.datastore.NavigationDataStoreOwner
import com.mapbox.navigation.ui.voice.internal.MapboxAudioGuidance
import com.mapbox.navigation.ui.voice.internal.impl.MapboxAudioGuidanceImpl
import java.util.concurrent.atomic.AtomicBoolean

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
object SharedApp {
    private var isSetup = false

    val store = Store()
    val state get() = store.state.value

    private val ignoreTripSessionUpdates = AtomicBoolean(false)

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
    val routeAlternativeStateController = RouteAlternativeStateController(store)
    private val navigationObservers: Array<MapboxNavigationObserver> = arrayOf(
        routeStateController,
        cameraStateController,
        locationStateController,
        navigationStateController,
        destinationStateController,
        routePreviewStateController,
        audioGuidanceStateController,
        routeAlternativeStateController,
        tripSessionStarterStateController,
    )

    fun setup(
        context: Context,
        audioGuidance: MapboxAudioGuidance? = null
    ) {
        if (isSetup) return
        isSetup = true

        MapboxNavigationApp.registerObserver(StateResetController(store, ignoreTripSessionUpdates))
        MapboxNavigationApp.lifecycleOwner.attachCreated(*navigationObservers)
        MapboxNavigationApp.registerObserver(audioGuidance ?: defaultAudioGuidance(context))
    }

    fun tripSessionTransaction(updateSession: () -> Unit) {
        // Any changes to MapboxNavigation TripSession should be done within `tripSessionTransaction { }` block.
        // This ensures that non of the registered TripSessionStateObserver accidentally execute cleanup logic
        // when TripSession state changes.
        ignoreTripSessionUpdates.set(true)
        updateSession()
        ignoreTripSessionUpdates.set(false)
    }

    private fun defaultAudioGuidance(context: Context): MapboxAudioGuidance {
        return MapboxAudioGuidanceImpl.create(context).also {
            it.dataStoreOwner = NavigationDataStoreOwner(context, DEFAULT_DATA_STORE_NAME)
        }
    }

    private const val DEFAULT_DATA_STORE_NAME = "mapbox_navigation_preferences"
}
