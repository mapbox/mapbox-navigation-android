package com.mapbox.navigation.dropin.component.camera

sealed class TargetCameraMode {
    object Idle : TargetCameraMode()
    object Overview : TargetCameraMode()
    object Following : TargetCameraMode()
}
