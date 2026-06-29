package com.mapbox.navigation.ui.maps.internal.camera

import androidx.annotation.RestrictTo
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.transition.NavigationCameraTransitionOptions

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun NavigationCamera.updateFollowingFrameTransitionOptions(
    frameTransitionOptions: NavigationCameraTransitionOptions,
) {
    updateFollowingFrameTransitionOptions(frameTransitionOptions)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun NavigationCamera.updateOverviewFrameTransitionOptions(
    frameTransitionOptions: NavigationCameraTransitionOptions,
) {
    updateOverviewFrameTransitionOptions(frameTransitionOptions)
}
