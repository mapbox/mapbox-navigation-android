package com.mapbox.navigation.dropin.component.camera

import com.mapbox.maps.EdgeInsets

data class CameraState internal constructor(
    val isCameraInitialized: Boolean = false,
    val cameraMode: TargetCameraMode = TargetCameraMode.Idle,
    val cameraPadding: EdgeInsets = EdgeInsets(0.0, 0.0, 0.0, 0.0)
)
