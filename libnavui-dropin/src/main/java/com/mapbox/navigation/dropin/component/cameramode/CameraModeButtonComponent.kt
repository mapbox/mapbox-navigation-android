package com.mapbox.navigation.dropin.component.cameramode

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.dropin.component.camera.DropInCameraMode
import com.mapbox.navigation.dropin.lifecycle.UIComponent
import com.mapbox.navigation.dropin.view.MapboxCameraModeButton
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState

@ExperimentalPreviewMapboxNavigationAPI
internal class CameraModeButtonComponent(
    private val cameraModeButton: MapboxCameraModeButton
) : UIComponent() {

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)
        val behaviour = MapboxNavigationApp.getObserver(CameraModeButtonBehaviour::class)

        behaviour.cameraMode.observe {
            cameraModeButton.setState(buttonState(it))
        }

        cameraModeButton.setOnClickListener {
            behaviour.onButtonClick()
        }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        cameraModeButton.setOnClickListener(null)
    }

    private fun buttonState(cameraMode: DropInCameraMode): NavigationCameraState =
        if (cameraMode == DropInCameraMode.FOLLOWING) NavigationCameraState.FOLLOWING
        else NavigationCameraState.OVERVIEW
}
