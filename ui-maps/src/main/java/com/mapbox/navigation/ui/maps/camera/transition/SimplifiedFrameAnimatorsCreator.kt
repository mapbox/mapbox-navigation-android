package com.mapbox.navigation.ui.maps.camera.transition

import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.plugin.animation.CameraAnimationsPlugin
import com.mapbox.navigation.ui.maps.internal.camera.SimplifiedUpdateFrameTransitionProvider

internal class SimplifiedFrameAnimatorsCreator(
    private val cameraAnimationsPlugin: CameraAnimationsPlugin,
    private val mapboxMap: MapboxMap,
    private val stateTransition: NavigationCameraStateTransition,
    private val simplifiedUpdateFrameTransition: SimplifiedUpdateFrameTransitionProvider,
) : AnimatorsCreator {

    override fun transitionToFollowing(
        cameraOptions: CameraOptions,
        transitionOptions: NavigationCameraTransitionOptions,
    ): FullAnimatorSet {
        return FullAnimatorSet(
            cameraAnimationsPlugin,
            mapboxMap,
            stateTransition.transitionToFollowing(cameraOptions, transitionOptions),
        )
    }

    override fun transitionToOverview(
        cameraOptions: CameraOptions,
        transitionOptions: NavigationCameraTransitionOptions,
    ): FullAnimatorSet {
        return FullAnimatorSet(
            cameraAnimationsPlugin,
            mapboxMap,
            stateTransition.transitionToOverview(cameraOptions, transitionOptions),
        )
    }

    override fun updateFrameForFollowing(
        cameraOptions: CameraOptions,
        transitionOptions: NavigationCameraTransitionOptions,
    ): MapboxAnimatorSet {
        return SimplifiedAnimatorSet(
            cameraAnimationsPlugin,
            mapboxMap,
            simplifiedUpdateFrameTransition.updateFollowingFrame(cameraOptions, transitionOptions),
        )
    }

    override fun updateFrameForOverview(
        cameraOptions: CameraOptions,
        transitionOptions: NavigationCameraTransitionOptions,
    ): MapboxAnimatorSet {
        return SimplifiedAnimatorSet(
            cameraAnimationsPlugin,
            mapboxMap,
            simplifiedUpdateFrameTransition.updateOverviewFrame(cameraOptions, transitionOptions),
        )
    }
}
