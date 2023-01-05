package com.mapbox.navigation.ui.app.internal

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
import com.mapbox.navigation.ui.app.internal.routefetch.RouteOptionsProvider
import com.mapbox.navigation.ui.maps.internal.ui.RouteAlternativeComponent
import com.mapbox.navigation.ui.maps.internal.ui.RouteAlternativeContract
import java.util.concurrent.atomic.AtomicBoolean

object SharedApp {
    private var isSetup = false

    val store = Store()
    val state get() = store.state.value
    val routeOptionsProvider: RouteOptionsProvider = RouteOptionsProvider()

    private val ignoreTripSessionUpdates = AtomicBoolean(false)

    private val navigationObservers: Array<MapboxNavigationObserver> = arrayOf(
        RouteStateController(store),
        CameraStateController(store),
        LocationStateController(store),
        NavigationStateController(store),
        DestinationStateController(store),
        RoutePreviewStateController(store, routeOptionsProvider),
        AudioGuidanceStateController(store),
        TripSessionStarterStateController(store),
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
