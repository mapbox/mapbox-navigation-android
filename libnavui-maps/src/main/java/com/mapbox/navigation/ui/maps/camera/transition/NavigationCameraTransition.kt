package com.mapbox.navigation.ui.maps.camera.transition

import android.animation.AnimatorSet
import androidx.annotation.UiThread
import com.mapbox.maps.CameraOptions

/**
 * Helper interface to provide camera transition animations.
 */
@UiThread
interface NavigationCameraTransition {
    /**
     * This transition moves the camera from a zoomed out level to a zoomed in level.
     *
     * The timings and delays are biased toward making sure the center point is visible
     * before zooming in and changing bearing and pitch.
     *
     * @param cameraOptions camera position to transition to
     * @param transitionOptions transition options
     */
    fun transitionFromLowZoomToHighZoom(
        cameraOptions: CameraOptions,
        transitionOptions: NavigationCameraTransitionOptions,
    ): AnimatorSet

    /**
     * This transition moves the camera from zoomed in to zoomed out.
     *
     * The timings and delays are made to favor zooming out first in order to
     * minimize fast moves over map geometry.
     *
     * @param cameraOptions camera position to transition to
     * @param transitionOptions transition options
     */
    fun transitionFromHighZoomToLowZoom(
        cameraOptions: CameraOptions,
        transitionOptions: NavigationCameraTransitionOptions,
    ): AnimatorSet

    /**
     * This transition is for use in frequently animating between points on a map.
     * No animation easing is used.
     *
     * This transition works best where frequent, continuous updates are needed.
     *
     * @param cameraOptions camera position to transition to
     * @param transitionOptions transition options
     */
    fun transitionLinear(
        cameraOptions: CameraOptions,
        transitionOptions: NavigationCameraTransitionOptions,
    ): AnimatorSet
}
