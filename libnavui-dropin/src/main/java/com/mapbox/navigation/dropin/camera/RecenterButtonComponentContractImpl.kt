package com.mapbox.navigation.dropin.camera

import android.view.View
import com.mapbox.navigation.ui.app.internal.Store
import com.mapbox.navigation.ui.app.internal.camera.CameraAction
import com.mapbox.navigation.ui.app.internal.camera.TargetCameraMode
import com.mapbox.navigation.ui.maps.internal.ui.RecenterButtonComponentContract
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow

internal class RecenterButtonComponentContractImpl(
    scope: CoroutineScope,
    private val store: Store
) : RecenterButtonComponentContract {

    override val isVisible: StateFlow<Boolean> =
        store.slice(scope = scope, started = SharingStarted.Eagerly) { state ->
            state.camera.cameraMode == TargetCameraMode.Idle
        }

    override fun onClick(view: View) {
        store.recenterCamera()
    }
}

internal fun Store.recenterCamera() = dispatch(
    CameraAction.SetCameraMode(TargetCameraMode.Following)
)
