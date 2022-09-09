package com.mapbox.navigation.ui.app.internal

import android.content.Context
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.extensions.attachCreated
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.ui.app.internal.controller.AudioGuidanceStateController
import com.mapbox.navigation.ui.app.internal.controller.CameraStateController
import com.mapbox.navigation.ui.app.internal.controller.DestinationStateController
import com.mapbox.navigation.ui.app.internal.controller.LocationStateController
import com.mapbox.navigation.ui.app.internal.controller.NavigationStateController
import com.mapbox.navigation.ui.app.internal.controller.RouteAlternativeComponentImpl
import com.mapbox.navigation.ui.app.internal.controller.RoutePreviewStateController
import com.mapbox.navigation.ui.app.internal.controller.RouteStateController
import com.mapbox.navigation.ui.app.internal.controller.StateResetController
import com.mapbox.navigation.ui.app.internal.controller.TripSessionStarterStateController
import com.mapbox.navigation.ui.maps.internal.ui.RouteAlternativeComponent
import com.mapbox.navigation.ui.maps.internal.ui.RouteAlternativeContract
import com.mapbox.navigation.ui.utils.internal.datastore.NavigationDataStoreOwner
import com.mapbox.navigation.ui.voice.internal.MapboxAudioGuidance
import com.mapbox.navigation.ui.voice.internal.impl.MapboxAudioGuidanceImpl
import java.util.concurrent.atomic.AtomicBoolean

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
object SharedApp : MapboxNavigationObserver {
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
    private val stateResetController = StateResetController(store, ignoreTripSessionUpdates)
    private val routeAlternativeComponent = RouteAlternativeComponent {
        RouteAlternativeComponentImpl(store)
    }
    private val navigationObservers: Array<MapboxNavigationObserver> = arrayOf(
        routeStateController,
        cameraStateController,
        locationStateController,
        navigationStateController,
        destinationStateController,
        routePreviewStateController,
        audioGuidanceStateController,
        tripSessionStarterStateController,
        stateResetController,
        routeAlternativeComponent,
    )

    /**
     * Requires the [Context] so will be initialized when attached.
     */
    private lateinit var mapboxAudioGuidance: MapboxAudioGuidance

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

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        val context = mapboxNavigation.navigationOptions.applicationContext
        mapboxAudioGuidance = defaultAudioGuidance(context)
        MapboxNavigationApp.registerObserver(mapboxAudioGuidance)
        MapboxNavigationApp.lifecycleOwner.attachCreated(*navigationObservers)
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        navigationObservers.forEach { MapboxNavigationApp.unregisterObserver(it) }
        MapboxNavigationApp.unregisterObserver(mapboxAudioGuidance)
    }

    private const val DEFAULT_DATA_STORE_NAME = "mapbox_navigation_preferences"
}
