package com.mapbox.navigation.qa_test_app.lifecycle

import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.lifecycle.LifecycleOwner
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.qa_test_app.lifecycle.viewmodel.DropInNavigationViewModel
import com.mapbox.navigation.ui.maps.camera.view.MapboxRecenterButton

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class DropInRecenterButton(
    private val viewModel: DropInNavigationViewModel,
    private val lifecycleOwner: LifecycleOwner, // TODO would like to remove this
    private val recenter: MapboxRecenterButton
) : MapboxNavigationObserver {

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        viewModel.cameraMode.observe(lifecycleOwner) {
            recenter.visibility = if (viewModel.isDefaultCameraMode()) {
                GONE
            } else {
                VISIBLE
            }
            recenter.setOnClickListener {
                viewModel.cameraMode.value = DropInNavigationViewModel.defaultCameraMode
            }
        }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        // no op
    }
}
