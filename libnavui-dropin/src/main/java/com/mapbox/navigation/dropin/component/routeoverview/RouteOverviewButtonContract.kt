package com.mapbox.navigation.dropin.component.routeoverview

import com.mapbox.navigation.dropin.component.navigationstate.NavigationState
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState

sealed class RouteOverviewButtonAction {
    data class UpdateNavigationState(
        val navigationState: NavigationState
    ) : RouteOverviewButtonAction()

    data class UpdateCameraState(
        val cameraState: NavigationCameraState
    ) : RouteOverviewButtonAction()
}

internal sealed class RouteOverviewButtonResult {
    data class OnNavigationState(
        val navigationState: NavigationState
    ) : RouteOverviewButtonResult()

    data class OnCameraState(
        val cameraState: NavigationCameraState
    ) : RouteOverviewButtonResult()

    data class OnVisibility(
        val isVisible: Boolean
    ) : RouteOverviewButtonResult()
}
