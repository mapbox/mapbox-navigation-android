package com.mapbox.navigation.dropin.component.routeoverview

import com.mapbox.navigation.dropin.component.navigationstate.NavigationState
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState

internal sealed interface RouteOverviewButtonProcessor {

    fun process(): RouteOverviewButtonResult

    data class ProcessNavigationState(
        val navigationState: NavigationState
    ) : RouteOverviewButtonProcessor {
        override fun process(): RouteOverviewButtonResult.OnNavigationState =
            RouteOverviewButtonResult.OnNavigationState(
                navigationState = navigationState
            )
    }

    data class ProcessCameraState(
        val cameraState: NavigationCameraState
    ) : RouteOverviewButtonProcessor {
        override fun process(): RouteOverviewButtonResult.OnCameraState =
            RouteOverviewButtonResult.OnCameraState(
                cameraState = cameraState
            )
    }

    data class ProcessVisibilityState(
        val navigationState: NavigationState,
        val cameraState: NavigationCameraState
    ) : RouteOverviewButtonProcessor {
        private val visibilitySet = setOf(
            NavigationState.RoutePreview,
            NavigationState.ActiveNavigation,
            NavigationState.Arrival,
            NavigationCameraState.IDLE,
            NavigationCameraState.TRANSITION_TO_FOLLOWING,
            NavigationCameraState.FOLLOWING
        )
        override fun process(): RouteOverviewButtonResult.OnVisibility =
            RouteOverviewButtonResult.OnVisibility(
                isVisible = visibilitySet.contains(navigationState) &&
                    visibilitySet.contains(cameraState)
            )
    }
}
