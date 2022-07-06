package com.mapbox.navigation.ui.app.internal.controller

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.app.internal.Action
import com.mapbox.navigation.ui.app.internal.State
import com.mapbox.navigation.ui.app.internal.Store
import com.mapbox.navigation.ui.app.internal.camera.CameraAction
import com.mapbox.navigation.ui.app.internal.camera.CameraState
import com.mapbox.navigation.ui.app.internal.camera.TargetCameraMode

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class CameraStateController(
    store: Store
) : StateController() {
    init {
        store.register(this)
    }

    override fun process(state: State, action: Action): State {
        if (action is CameraAction) {
            return state.copy(camera = processCameraAction(state.camera, action))
        }
        return state
    }

    private fun processCameraAction(state: CameraState, action: CameraAction): CameraState {
        return when (action) {
            is CameraAction.SetCameraMode -> {
                when (action.mode) {
                    TargetCameraMode.Idle ->
                        state.copy(cameraMode = action.mode, savedCameraMode = state.cameraMode)
                    else ->
                        state.copy(cameraMode = action.mode)
                }
            }
            is CameraAction.UpdatePadding -> {
                state.copy(cameraPadding = action.padding)
            }
            is CameraAction.SaveMapState -> {
                state.copy(mapCameraState = action.mapState)
            }
        }
    }
}
