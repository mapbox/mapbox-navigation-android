package com.mapbox.navigation.dropin.component.cameramode

import androidx.annotation.StyleRes
import androidx.core.view.isVisible
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.ui.app.internal.Store
import com.mapbox.navigation.ui.app.internal.camera.CameraAction.SetCameraMode
import com.mapbox.navigation.ui.app.internal.camera.TargetCameraMode
import com.mapbox.navigation.ui.app.internal.camera.toNavigationCameraState
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import com.mapbox.navigation.ui.maps.view.MapboxCameraModeButton

@ExperimentalPreviewMapboxNavigationAPI
internal class CameraModeButtonComponent(
    private val store: Store,
    private val cameraModeButton: MapboxCameraModeButton,
    @StyleRes private val cameraModeStyle: Int,
) : UIComponent() {

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        cameraModeButton.updateStyle(cameraModeStyle)
        store.select { it.camera.cameraMode }.observe { mode ->
            when (mode) {
                TargetCameraMode.Idle -> {
                    val savedMode = store.state.value.camera.savedCameraMode
                    cameraModeButton.setState(savedMode.toNavigationCameraState())
                }
                else -> {
                    cameraModeButton.setState(mode.toNavigationCameraState())
                }
            }
        }

        store.select { it.navigation }.observe {
            cameraModeButton.isVisible = it != NavigationState.RoutePreview
        }

        cameraModeButton.setOnClickListener {
            val cameraMode = store.state.value.camera.cameraMode.let {
                if (it != TargetCameraMode.Idle) it
                else store.state.value.camera.savedCameraMode
            }
            when (cameraMode) {
                TargetCameraMode.Following ->
                    store.dispatch(SetCameraMode(TargetCameraMode.Overview))
                else ->
                    store.dispatch(SetCameraMode(TargetCameraMode.Following))
            }
        }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        cameraModeButton.setOnClickListener(null)
    }
}
