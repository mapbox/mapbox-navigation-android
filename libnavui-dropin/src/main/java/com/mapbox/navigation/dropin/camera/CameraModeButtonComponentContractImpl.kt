package com.mapbox.navigation.dropin.camera

import android.view.View
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.app.internal.Store
import com.mapbox.navigation.ui.app.internal.camera.CameraAction
import com.mapbox.navigation.ui.app.internal.camera.TargetCameraMode
import com.mapbox.navigation.ui.app.internal.camera.toNavigationCameraState
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState
import com.mapbox.navigation.ui.maps.internal.ui.CameraModeButtonComponentContract
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

@ExperimentalPreviewMapboxNavigationAPI
internal class CameraModeButtonComponentContractImpl(
    coroutineScope: CoroutineScope,
    private val store: Store
) : CameraModeButtonComponentContract {

    override val buttonState: StateFlow<NavigationCameraState> =
        store.slice(coroutineScope) { state ->
            val cameraState = state.camera
            if (cameraState.cameraMode == TargetCameraMode.Idle) {
                cameraState.savedCameraMode.toNavigationCameraState()
            } else {
                cameraState.cameraMode.toNavigationCameraState()
            }
        }

    override val isVisible: StateFlow<Boolean> =
        store.slice(coroutineScope) { it.navigation != NavigationState.RoutePreview }

    override fun onClick(view: View) {
        val cameraMode = store.state.value.camera.cameraMode.let {
            if (it != TargetCameraMode.Idle) {
                it
            } else {
                store.state.value.camera.savedCameraMode
            }
        }
        when (cameraMode) {
            TargetCameraMode.Following ->
                store.dispatch(CameraAction.SetCameraMode(TargetCameraMode.Overview))
            else ->
                store.dispatch(CameraAction.SetCameraMode(TargetCameraMode.Following))
        }
    }
}
