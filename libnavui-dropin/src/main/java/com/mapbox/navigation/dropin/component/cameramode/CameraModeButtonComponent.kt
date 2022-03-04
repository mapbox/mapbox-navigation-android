package com.mapbox.navigation.dropin.component.cameramode

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.component.camera.CameraAction
import com.mapbox.navigation.dropin.component.camera.CameraMode
import com.mapbox.navigation.dropin.component.camera.CameraTransition
import com.mapbox.navigation.dropin.component.camera.CameraViewModel
import com.mapbox.navigation.dropin.lifecycle.UIComponent
import com.mapbox.navigation.dropin.view.MapboxCameraModeButton
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@ExperimentalPreviewMapboxNavigationAPI
internal class CameraModeButtonComponent(
    private val cameraViewModel: CameraViewModel,
    private val cameraModeButton: MapboxCameraModeButton,
) : UIComponent() {

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        coroutineScope.launch {
            cameraViewModel.state.collect {
                when (it.cameraMode) {
                    CameraMode.FOLLOWING -> {
                        cameraModeButton.setState(NavigationCameraState.FOLLOWING)
                    }
                    CameraMode.OVERVIEW -> {
                        cameraModeButton.setState(NavigationCameraState.OVERVIEW)
                    }
                    else -> {
                        // no op
                    }
                }
            }
        }

        cameraModeButton.setOnClickListener {
            when (cameraViewModel.state.value.cameraMode) {
                CameraMode.FOLLOWING -> {
                    cameraViewModel.invoke(
                        CameraAction.OnFollowingClicked(transitionTo = CameraTransition.ToOverview)
                    )
                }
                CameraMode.OVERVIEW -> {
                    cameraViewModel.invoke(
                        CameraAction.OnFollowingClicked(transitionTo = CameraTransition.ToFollowing)
                    )
                }
                else -> {
                    // no op
                }
            }
        }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        cameraModeButton.setOnClickListener(null)
    }
}
