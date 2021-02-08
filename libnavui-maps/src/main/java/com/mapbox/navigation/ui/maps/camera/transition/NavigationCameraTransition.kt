package com.mapbox.navigation.ui.maps.camera.transition

import android.animation.AnimatorSet
import com.mapbox.maps.CameraOptions

/**
 * Helper interface to provide camera transition animations.
 */
interface NavigationCameraTransition {
    /**
     * This transition moves the camera from a zoomed out level to a zoomed in level.
     *
     * The timings and delays are biased toward making sure the center point is visible
     * before zooming in and changing bearing and pitch.
     *
     * @param cameraOptions to transition to
     */
    fun transitionFromLowZoomToHighZoom(cameraOptions: CameraOptions): AnimatorSet

    /**
     * This transition moves the camera from zoomed in to zoomed out.
     *
     * The timings and delays are made to favor zooming out first in order to
     * minimize fast moves over map geometry.
     *
     * @param cameraOptions to transition to
     */
    fun transitionFromHighZoomToLowZoom(cameraOptions: CameraOptions): AnimatorSet

    /**
     * This transition is for use in frequently animating between points on a map.
     * No animation easing is used.
     *
     * This transition works best where frequent, continuous updates are needed.
     *
     * @param cameraOptions to transition to
     */
    fun transitionLinear(cameraOptions: CameraOptions): AnimatorSet
}
