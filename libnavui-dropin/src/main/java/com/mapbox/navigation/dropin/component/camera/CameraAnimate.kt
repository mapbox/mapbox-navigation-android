package com.mapbox.navigation.dropin.component.camera

sealed class CameraAnimate {
    object FlyTo : CameraAnimate()
    object SetTo : CameraAnimate()
    object EaseTo : CameraAnimate()
}

sealed class CameraTransition {
    object ToIdle : CameraTransition()
    object ToOverview : CameraTransition()
    object ToFollowing : CameraTransition()
}
