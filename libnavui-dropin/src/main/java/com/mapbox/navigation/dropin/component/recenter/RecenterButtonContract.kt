package com.mapbox.navigation.dropin.component.recenter

import com.mapbox.navigation.dropin.component.navigationstate.NavigationState
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState

sealed class RecenterButtonAction {
    data class UpdateNavigationState(
        val navigationState: NavigationState
    ) : RecenterButtonAction()

    data class UpdateCameraState(
        val cameraState: NavigationCameraState
    ) : RecenterButtonAction()
}

internal sealed class RecenterButtonResult {
    data class OnNavigationState(
        val navigationState: NavigationState
    ) : RecenterButtonResult()

    data class OnCameraState(
        val cameraState: NavigationCameraState
    ) : RecenterButtonResult()

    data class OnVisibility(
        val isVisible: Boolean
    ) : RecenterButtonResult()
}
