package com.mapbox.navigation.dropin

import androidx.lifecycle.ViewModel
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.dropin.component.audioguidance.AudioGuidanceViewModel
import com.mapbox.navigation.dropin.component.camera.CameraViewModel
import com.mapbox.navigation.dropin.component.destination.DestinationViewModel
import com.mapbox.navigation.dropin.component.location.LocationViewModel
import com.mapbox.navigation.dropin.component.navigation.NavigationState
import com.mapbox.navigation.dropin.component.navigation.NavigationStateViewModel
import com.mapbox.navigation.dropin.component.routefetch.RoutesViewModel
import com.mapbox.navigation.dropin.component.tripsession.TripSessionStarterViewModel

/**
 * There is a single ViewModel for the navigation view. Use this class to store state that should
 * survive orientation changes.
 */
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class DropInNavigationViewModel : ViewModel() {

    /**
     * These classes are accessible through MapboxNavigationApp.getObserver(..)
     */
    val navigationStateViewModel = NavigationStateViewModel(NavigationState.FreeDrive)
    val locationViewModel = LocationViewModel()
    val tripSessionStarterViewModel = TripSessionStarterViewModel(navigationStateViewModel)
    val audioGuidanceViewModel = AudioGuidanceViewModel(navigationStateViewModel)
    val cameraViewModel = CameraViewModel()
    val destinationViewModel = DestinationViewModel()
    val routesViewModel = RoutesViewModel()
    private val navigationObservers = listOf(
        destinationViewModel,
        tripSessionStarterViewModel,
        audioGuidanceViewModel,
        locationViewModel,
        routesViewModel,
        cameraViewModel,
        navigationStateViewModel,
        // TODO can add more mapbox navigation observers here
    )

    init {
        navigationObservers.forEach { MapboxNavigationApp.registerObserver(it) }
    }

    override fun onCleared() {
        navigationObservers.reversed().forEach { MapboxNavigationApp.unregisterObserver(it) }
    }
}
