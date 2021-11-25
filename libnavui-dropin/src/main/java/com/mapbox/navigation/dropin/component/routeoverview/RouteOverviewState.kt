package com.mapbox.navigation.dropin.component.routeoverview

import com.mapbox.navigation.dropin.component.navigationstate.NavigationState
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState

internal data class RouteOverviewState(
    val isVisible: Boolean,
    val navigationState: NavigationState,
    val cameraState: NavigationCameraState,
) {
    companion object {
        fun initial(): RouteOverviewState = RouteOverviewState(
            isVisible = false,
            navigationState = NavigationState.Empty,
            cameraState = NavigationCameraState.IDLE
        )
    }
}
