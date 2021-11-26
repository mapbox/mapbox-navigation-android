package com.mapbox.navigation.dropin.component.recenter

import com.mapbox.navigation.dropin.component.navigationstate.NavigationState
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState

internal sealed interface RecenterButtonProcessor {

    fun process(): RecenterButtonResult

    data class ProcessVisibilityState(
        val navigationState: NavigationState,
        val cameraState: NavigationCameraState,
        val cameraUpdatesInhibited: Boolean
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
                    (visibilitySet.contains(cameraState) || cameraUpdatesInhibited)
            )
    }
}
