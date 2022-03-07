package com.mapbox.navigation.dropin.component.camera

data class CameraState internal constructor(
    val isCameraInitialized: Boolean = false,
    val cameraMode: TargetCameraMode = TargetCameraMode.Idle,
)
