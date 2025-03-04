package com.mapbox.navigation.ui.maps.camera.transition

import com.mapbox.maps.CameraOptions

internal class SimplifiedFrameAnimatorsCreator(
    private val stateTransition: NavigationCameraStateTransition,
) : AnimatorsCreator {

    override fun transitionToFollowing(
        cameraOptions: CameraOptions,
        transitionOptions: NavigationCameraTransitionOptions,
    ): FullAnimatorSet {
        return FullAnimatorSet(
            stateTransition.transitionToFollowing(cameraOptions, transitionOptions),
        )
    }

    override fun transitionToOverview(
        cameraOptions: CameraOptions,
        transitionOptions: NavigationCameraTransitionOptions,
    ): FullAnimatorSet {
        return FullAnimatorSet(
            stateTransition.transitionToOverview(cameraOptions, transitionOptions),
        )
    }

    override fun updateFrameForFollowing(
        cameraOptions: CameraOptions,
        transitionOptions: NavigationCameraTransitionOptions,
    ): MapboxAnimatorSet {
        return SimplifiedAnimatorSet(
            stateTransition.updateFrameForFollowing(cameraOptions, transitionOptions)
                .childAnimations,
        )
    }

    override fun updateFrameForOverview(
        cameraOptions: CameraOptions,
        transitionOptions: NavigationCameraTransitionOptions,
    ): MapboxAnimatorSet {
        return SimplifiedAnimatorSet(
            stateTransition.updateFrameForOverview(cameraOptions, transitionOptions)
                .childAnimations,
        )
    }
}
