package com.mapbox.navigation.dropin.component.cameramode

import androidx.annotation.StyleRes
import androidx.core.view.isVisible
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.component.camera.CameraAction
import com.mapbox.navigation.dropin.component.camera.CameraViewModel
import com.mapbox.navigation.dropin.component.camera.TargetCameraMode
import com.mapbox.navigation.dropin.component.navigation.NavigationState
import com.mapbox.navigation.dropin.component.navigation.NavigationStateViewModel
import com.mapbox.navigation.dropin.lifecycle.UIComponent
import com.mapbox.navigation.dropin.view.MapboxCameraModeButton
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState

@ExperimentalPreviewMapboxNavigationAPI
internal class CameraModeButtonComponent(
    private val cameraViewModel: CameraViewModel,
    private val navigationStateViewModel: NavigationStateViewModel,
    private val cameraModeButton: MapboxCameraModeButton,
    @StyleRes private val cameraModeStyle: Int,
) : UIComponent() {

    private var buttonIconState: TargetCameraMode = TargetCameraMode.Idle

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        cameraModeButton.updateStyle(cameraModeStyle)
        cameraViewModel.state.observe {
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

        navigationStateViewModel.state.observe {
            cameraModeButton.isVisible = it != NavigationState.RoutePreview
        }

        cameraModeButton.setOnClickListener {
            when (cameraViewModel.state.value.cameraMode) {
                TargetCameraMode.Following -> {
                    cameraViewModel.invoke(
                        CameraAction.ToOverview
                    )
                }
                TargetCameraMode.Overview -> {
                    cameraViewModel.invoke(
                        CameraAction.ToFollowing
                    )
                }
                else -> {
                    when (buttonIconState) {
                        TargetCameraMode.Overview -> {
                            cameraViewModel.invoke(
                                CameraAction.ToOverview
                            )
                        }
                        TargetCameraMode.Following -> {
                            cameraViewModel.invoke(
                                CameraAction.ToFollowing
                            )
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
