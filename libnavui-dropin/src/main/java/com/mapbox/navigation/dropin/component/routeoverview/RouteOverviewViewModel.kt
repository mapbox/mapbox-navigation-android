package com.mapbox.navigation.dropin.component.routeoverview

import com.mapbox.navigation.dropin.component.DropInViewModel

internal class RouteOverviewViewModel :
    DropInViewModel<RouteOverviewState, RouteOverviewButtonAction>(
        initialState = RouteOverviewState.initial()
    ) {

    override suspend fun process(
        accumulator: RouteOverviewState,
        value: RouteOverviewButtonAction
    ): RouteOverviewState {
        return when (value) {
            is RouteOverviewButtonAction.UpdateNavigationState -> {
                val visibilityResult = RouteOverviewButtonProcessor.ProcessVisibilityState(
                    value.navigationState,
                    accumulator.cameraState
                ).process()
                val navigationStateResult = RouteOverviewButtonProcessor.ProcessNavigationState(
                    value.navigationState
                ).process()
                accumulator.copy(
                    isVisible = visibilityResult.isVisible,
                    navigationState = navigationStateResult.navigationState,
                    cameraState = accumulator.cameraState
                )
            }
            is RouteOverviewButtonAction.UpdateCameraState -> {
                val visibilityResult = RouteOverviewButtonProcessor.ProcessVisibilityState(
                    accumulator.navigationState,
                    value.cameraState
                ).process()
                val cameraStateResult = RouteOverviewButtonProcessor.ProcessCameraState(
                    value.cameraState
                ).process()
                accumulator.copy(
                    isVisible = visibilityResult.isVisible,
                    navigationState = accumulator.navigationState,
                    cameraState = cameraStateResult.cameraState
                )
            }
        }
    }
}
