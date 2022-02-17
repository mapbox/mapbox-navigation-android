package com.mapbox.navigation.dropin.component.cameramode

import com.mapbox.navigation.dropin.component.camera.DropInCameraMode
import com.mapbox.navigation.dropin.component.camera.DropInCameraState
import com.mapbox.navigation.dropin.lifecycle.UIComponent
import kotlinx.coroutines.flow.StateFlow

internal class CameraModeButtonBehaviour(
    private val cameraState: DropInCameraState
) : UIComponent() {

    val cameraMode: StateFlow<DropInCameraMode> = cameraState.cameraMode

    fun onButtonClick() {
        if (cameraState.cameraMode.value == DropInCameraMode.FOLLOWING) {
            cameraState.setCameraMode(DropInCameraMode.OVERVIEW)
        } else {
            cameraState.setCameraMode(DropInCameraMode.FOLLOWING)
        }
    }
}
