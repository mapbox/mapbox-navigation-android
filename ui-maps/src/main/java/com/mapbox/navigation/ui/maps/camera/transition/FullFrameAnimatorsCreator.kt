package com.mapbox.navigation.ui.maps.camera.transition

import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.plugin.animation.CameraAnimationsPlugin
import com.mapbox.navigation.ui.maps.internal.camera.transitionToPointsOverviewInternal

internal class FullFrameAnimatorsCreator(
    private val stateTransition: NavigationCameraStateTransition,
    private val cameraAnimationsPlugin: CameraAnimationsPlugin,
    private val mapboxMap: MapboxMap,
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

    override fun transitionToRouteOverview(
        cameraOptions: CameraOptions,
        transitionOptions: NavigationCameraTransitionOptions,
    ): FullAnimatorSet {
        return FullAnimatorSet(
            cameraAnimationsPlugin,
            mapboxMap,
            stateTransition.transitionToOverview(cameraOptions, transitionOptions),
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
                cameraPlugin = cameraAnimationsPlugin,
                mapboxMap = mapboxMap,
                cameraOptions = cameraOptions,
                transitionOptions = transitionOptions,
            ),
        )
    }

    override fun updateFrameForFollowing(
        cameraOptions: CameraOptions,
        transitionOptions: NavigationCameraTransitionOptions,
    ): MapboxAnimatorSet {
        return FullAnimatorSet(
            cameraAnimationsPlugin,
            mapboxMap,
            stateTransition.updateFrameForFollowing(cameraOptions, transitionOptions),
        )
    }

    override fun updateFrameForOverview(
        cameraOptions: CameraOptions,
        transitionOptions: NavigationCameraTransitionOptions,
    ): MapboxAnimatorSet {
        return FullAnimatorSet(
            cameraAnimationsPlugin,
            mapboxMap,
            stateTransition.updateFrameForOverview(cameraOptions, transitionOptions),
        )
    }
}
