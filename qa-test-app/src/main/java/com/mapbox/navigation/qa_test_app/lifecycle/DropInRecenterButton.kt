package com.mapbox.navigation.qa_test_app.lifecycle

import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.lifecycle.LifecycleOwner
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.qa_test_app.lifecycle.viewmodel.DropInNavigationCameraViewModel
import com.mapbox.navigation.ui.maps.camera.view.MapboxRecenterButton

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class DropInRecenterButton(
    private val cameraViewModel: DropInNavigationCameraViewModel,
    private val lifecycleOwner: LifecycleOwner, // TODO would like to remove this
    private val recenter: MapboxRecenterButton
) : MapboxNavigationObserver {

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        cameraViewModel.cameraMode.observe(lifecycleOwner) {
            recenter.visibility = if (cameraViewModel.isDefaultCameraMode()) {
                GONE
            } else {
                VISIBLE
            }
            recenter.setOnClickListener {
                cameraViewModel.cameraMode.value = DropInNavigationCameraViewModel.defaultCameraMode
            }
        }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        // no op
    }
}
