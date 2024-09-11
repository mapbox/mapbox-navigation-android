package com.mapbox.navigation.ui.maps.camera.transition

import android.animation.AnimatorSet
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.plugin.animation.CameraAnimationsPlugin

/**
 * Helper class that provides default implementation of [NavigationCameraStateTransition]
 * generators.
 */
class MapboxNavigationCameraStateTransition(
    private val mapboxMap: MapboxMap,
    cameraPlugin: CameraAnimationsPlugin,
    private val navigationCameraTransition: NavigationCameraTransition =
        MapboxNavigationCameraTransition(mapboxMap, cameraPlugin),
) : NavigationCameraStateTransition {

    override fun transitionToFollowing(
        cameraOptions: CameraOptions,
        transitionOptions: NavigationCameraTransitionOptions,
    ): AnimatorSet {
        return navigationCameraTransition.transitionFromLowZoomToHighZoom(
            cameraOptions,
            transitionOptions,
        )
    }

    override fun transitionToOverview(
        cameraOptions: CameraOptions,
        transitionOptions: NavigationCameraTransitionOptions,
    ): AnimatorSet {
        val currentZoom = mapboxMap.cameraState.zoom
        return if (currentZoom < cameraOptions.zoom ?: currentZoom) {
            navigationCameraTransition.transitionFromLowZoomToHighZoom(
                cameraOptions,
                transitionOptions,
            )
        } else {
            navigationCameraTransition.transitionFromHighZoomToLowZoom(
                cameraOptions,
                transitionOptions,
            )
        }
    }

    override fun updateFrameForFollowing(
        cameraOptions: CameraOptions,
        transitionOptions: NavigationCameraTransitionOptions,
    ): AnimatorSet {
        return navigationCameraTransition.transitionLinear(cameraOptions, transitionOptions)
    }

    override fun updateFrameForOverview(
        cameraOptions: CameraOptions,
        transitionOptions: NavigationCameraTransitionOptions,
    ): AnimatorSet {
        return navigationCameraTransition.transitionLinear(cameraOptions, transitionOptions)
    }
}
