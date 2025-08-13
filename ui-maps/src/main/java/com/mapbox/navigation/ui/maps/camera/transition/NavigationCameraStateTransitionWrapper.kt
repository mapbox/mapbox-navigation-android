package com.mapbox.navigation.ui.maps.camera.transition

import android.animation.AnimatorSet
import com.mapbox.maps.CameraOptions

internal class NavigationCameraStateTransitionWrapper(
    private val stateTransition: NavigationCameraStateTransition,
) : NavigationCameraStateTransitionProvider {

    override fun transitionToFollowing(
        cameraOptions: CameraOptions,
        transitionOptions: NavigationCameraTransitionOptions,
    ): AnimatorSet {
        return stateTransition.transitionToFollowing(cameraOptions, transitionOptions)
    }

    override fun transitionToOverview(
        cameraOptions: CameraOptions,
        transitionOptions: NavigationCameraTransitionOptions,
    ): AnimatorSet {
        return stateTransition.transitionToOverview(cameraOptions, transitionOptions)
    }
}
