package com.mapbox.navigation.dropin

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModel
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.internal.extensions.attachCreated
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.controller.AudioGuidanceStateController
import com.mapbox.navigation.dropin.controller.CameraStateController
import com.mapbox.navigation.dropin.controller.DestinationStateController
import com.mapbox.navigation.dropin.controller.LocationStateController
import com.mapbox.navigation.dropin.controller.NavigationStateController
import com.mapbox.navigation.dropin.controller.RoutesStateController
import com.mapbox.navigation.dropin.controller.TripSessionStarterStateController
import com.mapbox.navigation.dropin.model.Store

/**
 * There is a single ViewModel for the navigation view. Use this class to store state that should
 * survive configuration changes.
 */
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class NavigationViewModel : ViewModel() {

    val store = Store()

    /**
     * LifecycleOwner available for attaching events to a lifecycle that will survive configuration
     * changes. This is only available to the [NavigationViewModel] for now. We can consider
     * exposing the LifecycleOwner to downstream components, but we do not have a use for it yet.
     */
    private val viewModelLifecycleOwner = NavigationViewModelLifecycleOwner()

    /**
     * These classes are accessible through MapboxNavigationApp.getObserver(..)
     */
    private val navigationStateViewModel = NavigationStateController(store)
    val locationViewModel = LocationStateController(store)
    private val tripSessionStarterViewModel = TripSessionStarterStateController(store)
    private val audioGuidanceViewModel = AudioGuidanceStateController(store)
    private val cameraViewModel = CameraStateController(store)
    private val destinationViewModel = DestinationStateController(store)
    private val routesViewModel = RoutesStateController(store)
    private val navigationObservers: Array<MapboxNavigationObserver> = arrayOf(
        destinationViewModel,
        tripSessionStarterViewModel,
        audioGuidanceViewModel,
        locationViewModel,
        routesViewModel,
        cameraViewModel,
        navigationStateViewModel,
    )

    init {
        MapboxNavigationApp.attach(viewModelLifecycleOwner)
        viewModelLifecycleOwner.attachCreated(*navigationObservers)
    }

    override fun onCleared() {
        viewModelLifecycleOwner.destroy()
    }
}

/**
 * The [MapboxNavigationApp] needs a scope that can survive configuration changes.
 * Everything inside the [NavigationViewModel] will survive the orientation change, we can
 * assume that the [MapboxNavigationApp] is [Lifecycle.State.CREATED] between init and onCleared.
 *
 * The [Lifecycle.State.STARTED] and [Lifecycle.State.RESUMED] states are represented by the
 * hosting view [NavigationView].
 */
private class NavigationViewModelLifecycleOwner : LifecycleOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
        .apply { currentState = Lifecycle.State.CREATED }

    override fun getLifecycle(): Lifecycle = lifecycleRegistry

    fun destroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }
}
