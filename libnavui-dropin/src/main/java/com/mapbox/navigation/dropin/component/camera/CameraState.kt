package com.mapbox.navigation.dropin.component.camera

import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.navigation.ui.maps.camera.NavigationCamera

/**
 * Defines the target camera mode desired for navigation camera
 */
sealed class TargetCameraMode {
    /**
     * Represents Idle mode.
     */
    object Idle : TargetCameraMode()

    /**
     * Represents Overview mode.
     */
    object Overview : TargetCameraMode()

    /**
     * Represents Following mode.
     */
    object Following : TargetCameraMode()
}

/**
 * Defines the state for [NavigationCamera]
 * @property cameraMode sets the [TargetCameraMode]
 * @property cameraPadding sets the camera padding for the camera viewport
 * @property mapCameraState sets the [MapView] camera state
 */
data class CameraState internal constructor(
    val cameraMode: TargetCameraMode = TargetCameraMode.Idle,
    val cameraPadding: EdgeInsets = EdgeInsets(0.0, 0.0, 0.0, 0.0),
    val mapCameraState: com.mapbox.maps.CameraState? = null
)
