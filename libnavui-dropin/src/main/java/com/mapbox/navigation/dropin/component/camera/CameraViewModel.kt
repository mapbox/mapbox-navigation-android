package com.mapbox.navigation.dropin.component.camera

import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.lifecycle.UIViewModel

sealed class CameraAction {
    data class InitializeCamera(val target: TargetCameraMode) : CameraAction()
    object ToIdle : CameraAction()
    object ToOverview : CameraAction()
    object ToFollowing : CameraAction()
}

class CameraViewModel : UIViewModel<CameraState, CameraAction>(CameraState()) {

    override fun process(
        mapboxNavigation: MapboxNavigation,
        state: CameraState,
        action: CameraAction
    ): CameraState {

        return when (action) {
            is CameraAction.InitializeCamera -> {
                state.copy(isCameraInitialized = true, cameraMode = action.target)
            }
            is CameraAction.ToIdle -> {
                state.copy(cameraMode = TargetCameraMode.Idle)
            }
            is CameraAction.ToOverview -> {
                state.copy(cameraMode = TargetCameraMode.Overview)
            }
            is CameraAction.ToFollowing -> {
                state.copy(cameraMode = TargetCameraMode.Following)
            }
        }
    }
}
