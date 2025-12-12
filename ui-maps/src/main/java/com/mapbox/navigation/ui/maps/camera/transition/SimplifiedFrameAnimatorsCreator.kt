package com.mapbox.navigation.ui.maps.camera.transition

import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.plugin.animation.CameraAnimationsPlugin
import com.mapbox.navigation.ui.maps.internal.camera.SimplifiedUpdateFrameTransitionProvider
import com.mapbox.navigation.ui.maps.internal.camera.transitionToPointsOverviewInternal

internal class SimplifiedFrameAnimatorsCreator(
    private val cameraAnimationsPlugin: CameraAnimationsPlugin,
    private val mapboxMap: MapboxMap,
    private val stateTransitionProvider: NavigationCameraStateTransitionProvider,
    private val simplifiedUpdateFrameTransition: SimplifiedUpdateFrameTransitionProvider,
) : AnimatorsCreator {

    override fun transitionToFollowing(
        cameraOptions: CameraOptions,
        transitionOptions: NavigationCameraTransitionOptions,
    ): FullAnimatorSet {
        return FullAnimatorSet(
            cameraAnimationsPlugin,
            mapboxMap,
            stateTransitionProvider.transitionToFollowing(cameraOptions, transitionOptions),
        )
    }

    override fun transitionToRouteOverview(
        cameraOptions: CameraOptions,
        transitionOptions: NavigationCameraTransitionOptions,
    ): FullAnimatorSet {
        return FullAnimatorSet(
            cameraAnimationsPlugin,
            mapboxMap,
            stateTransitionProvider.transitionToOverview(cameraOptions, transitionOptions),
        )
    }

    override fun transitionToPointsOverview(
        cameraOptions: CameraOptions,
        transitionOptions: NavigationCameraTransitionOptions,
    ): FullAnimatorSet {
        return FullAnimatorSet(
            cameraAnimationsPlugin,
            mapboxMap,
            transitionToPointsOverviewInternal(
                cameraAnimationsPlugin,
                mapboxMap,
                cameraOptions,
                transitionOptions,
            ),
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
