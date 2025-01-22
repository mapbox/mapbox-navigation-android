package com.mapbox.navigation.ui.maps.camera.transition

import android.animation.AnimatorSet
import androidx.annotation.UiThread
import com.mapbox.maps.CameraOptions

/**
 * Helper interface to provide navigation camera state transitions.
 */
@UiThread
interface NavigationCameraStateTransition {
    /**
     * Transition the camera to following state.
     */
    fun transitionToFollowing(
        cameraOptions: CameraOptions,
        transitionOptions: NavigationCameraTransitionOptions,
    ): AnimatorSet

    /**
     * Transition the camera to overview state.
     */
    fun transitionToOverview(
        cameraOptions: CameraOptions,
        transitionOptions: NavigationCameraTransitionOptions,
    ): AnimatorSet

    /**
     * Transition that keeps following.
     */
    fun updateFrameForFollowing(
        cameraOptions: CameraOptions,
        transitionOptions: NavigationCameraTransitionOptions,
    ): AnimatorSet

    /**
     * Transition that keeps showing overview.
     */
    fun updateFrameForOverview(
        cameraOptions: CameraOptions,
        transitionOptions: NavigationCameraTransitionOptions,
    ): AnimatorSet
}
