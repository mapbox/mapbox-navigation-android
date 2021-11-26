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
                    accumulator.cameraState,
                    accumulator.cameraUpdatesInhibited,
                ).process()
                accumulator.copy(
                    isVisible = visibilityResult.isVisible,
                    navigationState = value.navigationState,
                )
            }
            is RecenterButtonAction.UpdateCameraState -> {
                val visibilityResult = RecenterButtonProcessor.ProcessVisibilityState(
                    accumulator.navigationState,
                    value.cameraState,
                    accumulator.cameraUpdatesInhibited,
                ).process()
                accumulator.copy(
                    isVisible = visibilityResult.isVisible,
                    cameraState = value.cameraState,
                )
            }
            is RecenterButtonAction.UpdateCameraUpdatesInhibitedState -> {
                val visibilityResult = RecenterButtonProcessor.ProcessVisibilityState(
                    accumulator.navigationState,
                    accumulator.cameraState,
                    value.cameraUpdatesInhibited,
                ).process()
                accumulator.copy(
                    isVisible = visibilityResult.isVisible,
                    cameraUpdatesInhibited = value.cameraUpdatesInhibited,
                )
            }
        }
    }
}
