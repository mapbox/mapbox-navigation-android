package com.mapbox.navigation.dropin.component.recenter

import android.view.View.GONE
import android.view.View.VISIBLE
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.component.camera.CameraAction
import com.mapbox.navigation.dropin.component.camera.CameraAnimate
import com.mapbox.navigation.dropin.component.camera.CameraMode
import com.mapbox.navigation.dropin.component.camera.CameraViewModel
import com.mapbox.navigation.dropin.lifecycle.UIComponent
import com.mapbox.navigation.dropin.view.MapboxExtendableButton
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@ExperimentalPreviewMapboxNavigationAPI
internal class RecenterButtonComponent(
    private val cameraViewModel: CameraViewModel,
    private val recenterButton: MapboxExtendableButton,
) : UIComponent() {

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        coroutineScope.launch {
            cameraViewModel.state.collect {
                if (it.cameraMode == CameraMode.IDLE) {
                    recenterButton.visibility = VISIBLE
                } else {
                    recenterButton.visibility = GONE
                }
            }
        }
        recenterButton.setOnClickListener {
            cameraViewModel.invoke(
                CameraAction.OnRecenterClicked(animation = CameraAnimate.EaseTo)
            )
        }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        recenterButton.setOnClickListener(null)
    }
}
