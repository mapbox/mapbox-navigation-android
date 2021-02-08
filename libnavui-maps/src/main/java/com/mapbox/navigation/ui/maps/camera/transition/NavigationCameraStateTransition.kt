package com.mapbox.navigation.ui.maps.camera.transition

import android.animation.AnimatorSet
import com.mapbox.maps.CameraOptions

/**
 * Helper interface to provide navigation camera state transitions.
 */
interface NavigationCameraStateTransition {
    /**
     * Transition the camera to following state.
     */
    fun transitionToFollowing(cameraOptions: CameraOptions): AnimatorSet

    /**
     * Transition the camera to overview state.
     */
    fun transitionToOverview(cameraOptions: CameraOptions): AnimatorSet

    /**
     * Transition that keeps following.
     */
    fun updateFrameForFollowing(cameraOptions: CameraOptions): AnimatorSet

    /**
     * Transition that keeps showing overview.
     */
    fun updateFrameForOverview(cameraOptions: CameraOptions): AnimatorSet
}
