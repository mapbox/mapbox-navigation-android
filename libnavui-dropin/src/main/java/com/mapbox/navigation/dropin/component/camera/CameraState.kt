package com.mapbox.navigation.dropin.component.camera

import android.location.Location
import com.mapbox.maps.CameraOptions

data class CameraState(
    val location: Location?,
    val cameraMode: CameraMode,
    val recenterTo: CameraMode,
    val cameraOptions: CameraOptions,
    val cameraAnimation: CameraAnimate,
    val cameraTransition: CameraTransition,
) {
    companion object {
        private const val DEFAULT_INITIAL_ZOOM = 15.0
        fun initial() = CameraState(
            location = null,
            cameraMode = CameraMode.OVERVIEW,
            recenterTo = CameraMode.OVERVIEW,
            cameraOptions = CameraOptions.Builder().zoom(DEFAULT_INITIAL_ZOOM).build(),
            cameraAnimation = CameraAnimate.SetTo,
            cameraTransition = CameraTransition.ToOverview,
        )
    }
}
