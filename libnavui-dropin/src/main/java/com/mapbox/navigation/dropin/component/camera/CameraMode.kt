package com.mapbox.navigation.dropin.component.camera

sealed class CameraMode {
    object IDLE : CameraMode()
    object OVERVIEW : CameraMode()
    object FOLLOWING : CameraMode()
}
