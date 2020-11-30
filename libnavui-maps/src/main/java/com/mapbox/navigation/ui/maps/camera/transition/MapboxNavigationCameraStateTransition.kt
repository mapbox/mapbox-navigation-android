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
        MapboxNavigationCameraTransition(mapboxMap, cameraPlugin)
) : NavigationCameraStateTransition {

    override fun transitionToFollowing(cameraOptions: CameraOptions): AnimatorSet {
        return navigationCameraTransition.transitionFromLowZoomToHighZoom(cameraOptions)
    }

    override fun transitionToOverview(cameraOptions: CameraOptions): AnimatorSet {
        val currentZoom = mapboxMap.getCameraOptions(null).zoom ?: 0.0
        return if (currentZoom < cameraOptions.zoom ?: currentZoom) {
            navigationCameraTransition.transitionFromLowZoomToHighZoom(cameraOptions)
        } else {
            navigationCameraTransition.transitionFromHighZoomToLowZoom(cameraOptions)
        }
    }

    override fun updateFrameForFollowing(cameraOptions: CameraOptions): AnimatorSet {
        return navigationCameraTransition.transitionLinear(cameraOptions)
    }

    override fun updateFrameForOverview(cameraOptions: CameraOptions): AnimatorSet {
        return navigationCameraTransition.transitionLinear(cameraOptions)
    }
}
