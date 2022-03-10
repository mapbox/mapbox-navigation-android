package com.mapbox.navigation.dropin

import androidx.lifecycle.ViewModel
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.dropin.component.audioguidance.AudioGuidanceViewModel
import com.mapbox.navigation.dropin.component.camera.CameraViewModel
import com.mapbox.navigation.dropin.component.location.LocationViewModel
import com.mapbox.navigation.dropin.component.navigationstate.NavigationState
import com.mapbox.navigation.dropin.component.replay.ReplayViewModel
import com.mapbox.navigation.dropin.component.routefetch.RoutesViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * There is a single ViewModel for the navigation view. Use this class to store state that should
 * survive orientation changes.
 */
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class DropInNavigationViewModel : ViewModel() {
    private val _navigationState = MutableStateFlow<NavigationState>(NavigationState.FreeDrive)
    val navigationState = _navigationState.asStateFlow()

    /**
     * These classes are accessible through MapboxNavigationApp.getObserver(..)
     */
    val replayViewModel = ReplayViewModel()
    val audioGuidanceViewModel = AudioGuidanceViewModel()
    val locationViewModel = LocationViewModel()
    val routesViewModel = RoutesViewModel()
    val cameraViewModel = CameraViewModel()
    val navigationObservers = listOf(
        replayViewModel,
        audioGuidanceViewModel,
        locationViewModel,
        routesViewModel,
        cameraViewModel,
        // TODO can add more mapbox navigation observers here
    )

    fun updateState(state: NavigationState) {
        _navigationState.value = state
    }

    init {
        navigationObservers.forEach { MapboxNavigationApp.registerObserver(it) }
    }

    override fun onCleared() {
        navigationObservers.reversed().forEach { MapboxNavigationApp.unregisterObserver(it) }
    }
}
