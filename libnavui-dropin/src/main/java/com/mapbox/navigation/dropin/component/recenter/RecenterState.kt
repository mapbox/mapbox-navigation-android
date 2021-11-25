package com.mapbox.navigation.dropin.component.recenter

import com.mapbox.navigation.dropin.component.navigationstate.NavigationState
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState

internal data class RecenterState(
    val isVisible: Boolean,
    val navigationState: NavigationState,
    val cameraState: NavigationCameraState,
) {
    companion object {
        fun initial(): RecenterState = RecenterState(
            isVisible = false,
            navigationState = NavigationState.Empty,
            cameraState = NavigationCameraState.IDLE
        )
    }
}
