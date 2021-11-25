package com.mapbox.navigation.dropin.component.recenter

import com.mapbox.navigation.dropin.component.DropInViewModel

internal class RecenterViewModel : DropInViewModel<RecenterState, RecenterButtonAction>(
    initialState = RecenterState.initial()
) {

    override suspend fun process(
        accumulator: RecenterState,
        value: RecenterButtonAction
    ): RecenterState {
        return when (value) {
            is RecenterButtonAction.UpdateNavigationState -> {
                val visibilityResult = RecenterButtonProcessor.ProcessVisibilityState(
                    value.navigationState,
                    accumulator.cameraState
                ).process()
                val navigationStateResult = RecenterButtonProcessor.ProcessNavigationState(
                    value.navigationState
                ).process()
                accumulator.copy(
                    isVisible = visibilityResult.isVisible,
                    navigationState = navigationStateResult.navigationState,
                    cameraState = accumulator.cameraState
                )
            }
            is RecenterButtonAction.UpdateCameraState -> {
                val visibilityResult = RecenterButtonProcessor.ProcessVisibilityState(
                    accumulator.navigationState,
                    value.cameraState
                ).process()
                val cameraStateResult = RecenterButtonProcessor.ProcessCameraState(
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
