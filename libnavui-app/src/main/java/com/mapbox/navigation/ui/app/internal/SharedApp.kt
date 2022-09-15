package com.mapbox.navigation.ui.app.internal

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
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
import com.mapbox.navigation.ui.voice.api.MapboxAudioGuidance
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
    private val navigationObservers: Array<MapboxNavigationObserver> = arrayOf(
        routeStateController,
        cameraStateController,
        locationStateController,
        navigationStateController,
        destinationStateController,
        routePreviewStateController,
        audioGuidanceStateController,
        tripSessionStarterStateController,
    )

    @JvmOverloads
    fun setup(
        routeAlternativeContract: RouteAlternativeContract? = null
    ) {
        if (isSetup) return
        isSetup = true

        MapboxNavigationApp.registerObserver(StateResetController(store, ignoreTripSessionUpdates))
        MapboxNavigationApp.registerObserver(
            RouteAlternativeComponent {
                routeAlternativeContract ?: RouteAlternativeComponentImpl(store)
            }
        )
        MapboxNavigationApp.lifecycleOwner.attachCreated(*navigationObservers)

        // TODO Remove this from SharedApp. The components that use `MapboxAudioGuidance`
        //   will "ensureAudioGuidanceRegistered". See the `ComponentInstaller.audioGuidanceButton`
        if (MapboxNavigationApp.getObservers(MapboxAudioGuidance::class).isEmpty()) {
            MapboxNavigationApp.registerObserver(MapboxAudioGuidance())
        }
    }

    fun tripSessionTransaction(updateSession: () -> Unit) {
        // Any changes to MapboxNavigation TripSession should be done within `tripSessionTransaction { }` block.
        // This ensures that non of the registered TripSessionStateObserver accidentally execute cleanup logic
        // when TripSession state changes.
        ignoreTripSessionUpdates.set(true)
        updateSession()
        ignoreTripSessionUpdates.set(false)
    }
}
