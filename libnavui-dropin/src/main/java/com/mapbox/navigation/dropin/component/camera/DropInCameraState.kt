package com.mapbox.navigation.dropin.component.camera

import androidx.lifecycle.MutableLiveData
import com.mapbox.maps.CameraOptions
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class DropInCameraState {
    val cameraMode = MutableLiveData(defaultCameraMode)
    val cameraOptions = MutableLiveData(defaultCameraOptions)
    var triggerIdleCameraOnMoveListener: Boolean = true

    fun cameraMode() = cameraMode.value ?: defaultCameraMode

    companion object {
        val defaultCameraMode: DropInCameraMode = DropInCameraMode.FOLLOWING

        private const val DEFAULT_INITIAL_ZOOM = 15.0
        private val defaultCameraOptions = CameraOptions.Builder()
            .zoom(DEFAULT_INITIAL_ZOOM)
    }
}
