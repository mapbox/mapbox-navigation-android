package com.mapbox.navigation.dropin

import androidx.lifecycle.ViewModel
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.dropin.component.camera.DropInCameraState
import com.mapbox.navigation.dropin.component.navigationstate.NavigationState
import com.mapbox.navigation.dropin.component.replay.DropInReplayComponent
import com.mapbox.navigation.dropin.lifecycle.navigationListOf
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

    val cameraState: DropInCameraState = DropInCameraState()
    val replayComponent = DropInReplayComponent()

    fun updateState(state: NavigationState) {
        _navigationState.value = state
    }

    val navigationObservers = navigationListOf(
        replayComponent,
        // TODO can add more mapbox navigation observers here
    )

    init {
        MapboxNavigationApp.registerObserver(navigationObservers)
    }

    override fun onCleared() {
        MapboxNavigationApp.unregisterObserver(navigationObservers)
    }
}
