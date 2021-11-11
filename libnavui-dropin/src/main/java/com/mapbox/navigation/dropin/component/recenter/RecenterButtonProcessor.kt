package com.mapbox.navigation.dropin.component.recenter

import com.mapbox.navigation.dropin.component.navigationstate.NavigationState
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState

internal sealed interface RecenterButtonProcessor {

    fun process(): RecenterButtonResult

    data class ProcessNavigationState(
        val navigationState: NavigationState
    ) : RecenterButtonProcessor {
        override fun process(): RecenterButtonResult.OnNavigationState =
            RecenterButtonResult.OnNavigationState(
                navigationState = navigationState
            )
    }

    data class ProcessCameraState(
        val cameraState: NavigationCameraState
    ) : RecenterButtonProcessor {
        override fun process(): RecenterButtonResult.OnCameraState =
            RecenterButtonResult.OnCameraState(
                cameraState = cameraState
            )
    }

    data class ProcessVisibilityState(
        val navigationState: NavigationState,
        val cameraState: NavigationCameraState
    ) : RecenterButtonProcessor {
        private val visibilitySet = setOf(
            NavigationState.FreeDrive,
            NavigationState.RoutePreview,
            NavigationState.ActiveNavigation,
            NavigationState.Arrival,
            NavigationCameraState.IDLE,
            NavigationCameraState.TRANSITION_TO_OVERVIEW,
            NavigationCameraState.OVERVIEW
        )
        override fun process(): RecenterButtonResult.OnVisibility =
            RecenterButtonResult.OnVisibility(
                isVisible = visibilitySet.contains(navigationState) &&
                    visibilitySet.contains(cameraState)
            )
    }
}
