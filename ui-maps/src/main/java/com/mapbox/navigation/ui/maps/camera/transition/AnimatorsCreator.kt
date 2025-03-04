package com.mapbox.navigation.ui.maps.camera.transition

import com.mapbox.maps.CameraOptions

internal sealed interface AnimatorsCreator {

    fun transitionToFollowing(
        cameraOptions: CameraOptions,
        transitionOptions: NavigationCameraTransitionOptions,
    ): FullAnimatorSet

    fun transitionToOverview(
        cameraOptions: CameraOptions,
        transitionOptions: NavigationCameraTransitionOptions,
    ): FullAnimatorSet

    fun updateFrameForFollowing(
        cameraOptions: CameraOptions,
        transitionOptions: NavigationCameraTransitionOptions,
    ): MapboxAnimatorSet

    fun updateFrameForOverview(
        cameraOptions: CameraOptions,
        transitionOptions: NavigationCameraTransitionOptions,
    ): MapboxAnimatorSet
}
