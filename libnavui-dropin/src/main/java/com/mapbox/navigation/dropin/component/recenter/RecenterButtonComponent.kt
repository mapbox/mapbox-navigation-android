package com.mapbox.navigation.dropin.component.recenter

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
import com.mapbox.navigation.dropin.view.MapboxExtendableButton
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@ExperimentalPreviewMapboxNavigationAPI
internal class RecenterButtonComponent(
    private val cameraViewModel: CameraViewModel,
    private val navigationStateViewModel: NavigationStateViewModel,
    @StyleRes private val recenterStyle: Int,
    private val recenterButton: MapboxExtendableButton,
) : UIComponent() {

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        recenterButton.updateStyle(recenterStyle)
        recenterButton.setOnClickListener {
            cameraViewModel.invoke(
                CameraAction.ToFollowing
            )
        }

        coroutineScope.launch {
            combine(
                cameraViewModel.state,
                navigationStateViewModel.state
            ) { cameraState, navigationState ->
                navigationState != NavigationState.RoutePreview &&
                    cameraState.cameraMode == TargetCameraMode.Idle
            }.collect { visible ->
                recenterButton.isVisible = visible
            }
        }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        recenterButton.setOnClickListener(null)
    }
}
