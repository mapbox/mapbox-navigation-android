package com.mapbox.navigation.dropin.component.cameramode

import androidx.annotation.StyleRes
import androidx.core.view.isVisible
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.component.camera.CameraAction
import com.mapbox.navigation.dropin.component.camera.TargetCameraMode
import com.mapbox.navigation.dropin.component.navigation.NavigationState
import com.mapbox.navigation.dropin.lifecycle.UIComponent
import com.mapbox.navigation.dropin.model.Store
import com.mapbox.navigation.dropin.view.MapboxCameraModeButton
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState

@ExperimentalPreviewMapboxNavigationAPI
internal class CameraModeButtonComponent(
    private val store: Store,
    private val cameraModeButton: MapboxCameraModeButton,
    @StyleRes private val cameraModeStyle: Int,
) : UIComponent() {

    private var buttonIconState: TargetCameraMode = TargetCameraMode.Idle

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        cameraModeButton.updateStyle(cameraModeStyle)
        store.select { it.camera }.observe {
            when (it.cameraMode) {
                TargetCameraMode.Following -> {
                    buttonIconState = TargetCameraMode.Overview
                    cameraModeButton.setState(NavigationCameraState.FOLLOWING)
                }
                TargetCameraMode.Overview -> {
                    buttonIconState = TargetCameraMode.Following
                    cameraModeButton.setState(NavigationCameraState.OVERVIEW)
                }
                else -> {
                    // no op
                }
            }
        }

        store.select { it.navigation }.observe {
            cameraModeButton.isVisible = it != NavigationState.RoutePreview
        }

        cameraModeButton.setOnClickListener {
            when (store.state.value.camera.cameraMode) {
                TargetCameraMode.Following -> {
                    store.dispatch(CameraAction.ToOverview)
                }
                TargetCameraMode.Overview -> {
                    store.dispatch(CameraAction.ToFollowing)
                }
                else -> {
                    when (buttonIconState) {
                        TargetCameraMode.Overview -> {
                            store.dispatch(CameraAction.ToOverview)
                        }
                        TargetCameraMode.Following -> {
                            store.dispatch(CameraAction.ToFollowing)
                        }
                        else -> {
                            // no op
                        }
                    }
                }
            }
        }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        cameraModeButton.setOnClickListener(null)
    }
}
