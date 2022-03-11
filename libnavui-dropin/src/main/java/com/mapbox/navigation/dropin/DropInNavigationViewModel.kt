package com.mapbox.navigation.dropin

import androidx.lifecycle.ViewModel
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.dropin.component.audioguidance.AudioGuidanceViewModel
import com.mapbox.navigation.dropin.component.camera.CameraViewModel
import com.mapbox.navigation.dropin.component.destination.DestinationViewModel
import com.mapbox.navigation.dropin.component.location.LocationViewModel
import com.mapbox.navigation.dropin.component.navigation.NavigationStateComponent
import com.mapbox.navigation.dropin.component.navigation.NavigationStateViewModel
import com.mapbox.navigation.dropin.component.navigationstate.NavigationState
import com.mapbox.navigation.dropin.component.replay.ReplayViewModel
import com.mapbox.navigation.dropin.component.routefetch.RoutesViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * There is a single ViewModel for the navigation view. Use this class to store state that should
 * survive orientation changes.
 */
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class DropInNavigationViewModel : ViewModel() {

    private val _onBackPressedEvent = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val onBackPressedEvent = _onBackPressedEvent.asSharedFlow()

    /**
     * These classes are accessible through MapboxNavigationApp.getObserver(..)
     */
    val replayViewModel = ReplayViewModel()
    val audioGuidanceViewModel = AudioGuidanceViewModel()
    val locationViewModel = LocationViewModel()

    val cameraViewModel = CameraViewModel()
    val navigationStateViewModel = NavigationStateViewModel(NavigationState.FreeDrive)
    val destinationViewModel = DestinationViewModel()
    val routesViewModel = RoutesViewModel(
        navigationStateViewModel,
        locationViewModel,
        destinationViewModel,
    )
    val navigationStateComponent = NavigationStateComponent(
        navigationStateViewModel,
        destinationViewModel,
        routesViewModel
    )
    private val navigationObservers = listOf(
        destinationViewModel,
        replayViewModel,
        audioGuidanceViewModel,
        locationViewModel,
        routesViewModel,
        cameraViewModel,
        navigationStateViewModel,
        navigationStateComponent,
        // TODO can add more mapbox navigation observers here
    )

    fun onBackPressed() {
        _onBackPressedEvent.tryEmit(Unit)
    }

    init {
        navigationObservers.forEach { MapboxNavigationApp.registerObserver(it) }
    }

    override fun onCleared() {
        navigationObservers.reversed().forEach { MapboxNavigationApp.unregisterObserver(it) }
    }
}
