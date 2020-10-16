package com.mapbox.navigation.ui.maps.camera

import android.animation.AnimatorSet

/**
 * Helper interface to provide camera transition animation
 */
interface NavigationCameraTransitionProvider {
    /**
     * This transition moves the camera from a zoomed out level to a zoomed in level.
     * The timings and delays are biased toward making sure the center point is visible
     * before zooming in and changing bearing and pitch.
     *
     * @param transitionOptions transition options including center/zoom/pitch etc.
     *  see [NavigationCameraZoomTransitionOptions]
     */
    fun transitionFromLowZoomToHighZoom(
        transitionOptions: NavigationCameraZoomTransitionOptions): AnimatorSet

    /**
     * This transition moves the camera from zoomed in to zoomed out.
     * The timings and delays are made to favor zooming out first in order to
     * minimize fast moves over map geometry.
     *
     * @param transitionOptions transition options including center/zoom/pitch etc.
     *  see [NavigationCameraZoomTransitionOptions]
     */
    fun transitionFromHighZoomToLowZoom(
        transitionOptions: NavigationCameraZoomTransitionOptions): AnimatorSet

    /**
     * This transition is for use in frequently animating between points on a map.
     * No animation easing is used.
     * This transition works best where frequent updates to a location are needed.
     *
     * @param transitionOptions transition options including center/zoom/pitch etc.
     *  see [NavigationCameraLinearTransitionOptions]
     */
    fun transitionLinear(
        transitionOptions: NavigationCameraLinearTransitionOptions): AnimatorSet
}
