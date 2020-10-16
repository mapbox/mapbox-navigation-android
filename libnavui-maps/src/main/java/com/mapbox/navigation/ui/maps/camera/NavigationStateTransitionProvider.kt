package com.mapbox.navigation.ui.maps.camera

import android.animation.AnimatorSet

/**
 * Helper interface to provide navigation camera state transition
 *
 * Define the camera parameters and pass them into the [NavigationCameraTransitionProvider]
 * to do the camera state transition.
 */
interface NavigationStateTransitionProvider {
    /**
     * Transition the camera to vehicle following state
     *
     * @param transitionOptions transition options including vehicle location/points/pitch etc.
     *  see [NavigationStateTransitionToFollowingOptions]
     */
    fun transitionToVehicleFollowing(
        transitionOptions: NavigationStateTransitionToFollowingOptions
    ): AnimatorSet

    /**
     * Transition the camera to route overview state
     *
     * @param transitionOptions transition options including vehicle location/points/pitch etc.
     *  see [NavigationStateTransitionToRouteOverviewOptions]
     */
    fun transitionToRouteOverview(
        transitionOptions: NavigationStateTransitionToRouteOverviewOptions
    ): AnimatorSet

    /**
     * Get the next transition that keeps following the vehicle
     *
     * @param transitionOptions transition options including vehicle location/points/pitch etc.
     *  see [NavigationStateTransitionToFollowingOptions]
     */
    fun updateMapFrameForFollowing(transitionOptions: NavigationStateTransitionToFollowingOptions
    ): AnimatorSet

    /**
     * Get the next transition that shows route overview
     *
     * @param transitionOptions transition options including vehicle location/points/pitch etc.
     *  see [NavigationStateTransitionToRouteOverviewOptions]
     */
    fun updateMapFrameForOverview(transitionOptions: NavigationStateTransitionToRouteOverviewOptions
    ): AnimatorSet
}
