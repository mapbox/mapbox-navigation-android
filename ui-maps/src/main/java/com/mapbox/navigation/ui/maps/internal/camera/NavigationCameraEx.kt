package com.mapbox.navigation.ui.maps.internal.camera

import androidx.annotation.RestrictTo
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.NavigationCamera.Companion.DEFAULT_FRAME_TRANSITION_OPT
import com.mapbox.navigation.ui.maps.camera.NavigationCamera.Companion.DEFAULT_STATE_TRANSITION_OPT
import com.mapbox.navigation.ui.maps.camera.transition.NavigationCameraTransitionOptions
import com.mapbox.navigation.ui.maps.camera.transition.TransitionEndListener

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun NavigationCamera.updateFollowingFrameTransitionOptions(
    frameTransitionOptions: NavigationCameraTransitionOptions,
) {
    updateFollowingFrameTransitionOptions(frameTransitionOptions)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun NavigationCamera.requestNavigationCameraToPointsOverview(
    stateTransitionOptions: NavigationCameraTransitionOptions = DEFAULT_STATE_TRANSITION_OPT,
    frameTransitionOptions: NavigationCameraTransitionOptions = DEFAULT_FRAME_TRANSITION_OPT,
    transitionEndListener: TransitionEndListener? = null,
) {
    requestNavigationCameraToPointsOverview(
        stateTransitionOptions,
        frameTransitionOptions,
        transitionEndListener,
    )
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun NavigationCamera.updateOverviewFrameTransitionOptions(
    frameTransitionOptions: NavigationCameraTransitionOptions,
) {
    updateOverviewFrameTransitionOptions(frameTransitionOptions)
}
