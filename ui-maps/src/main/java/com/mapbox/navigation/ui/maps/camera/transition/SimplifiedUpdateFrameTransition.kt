package com.mapbox.navigation.ui.maps.camera.transition

import android.animation.ValueAnimator
import androidx.core.view.animation.PathInterpolatorCompat
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.plugin.animation.CameraAnimationsPlugin
import com.mapbox.maps.plugin.animation.CameraAnimatorOptions
import com.mapbox.navigation.ui.maps.camera.NavigationCamera.Companion.NAVIGATION_CAMERA_OWNER
import com.mapbox.navigation.ui.maps.internal.camera.normalizeBearing

internal class SimplifiedUpdateFrameTransition(
    private val mapboxMap: MapboxMap,
    private val cameraPlugin: CameraAnimationsPlugin,
) {

    fun updateFrame(
        cameraOptions: CameraOptions,
        transitionOptions: NavigationCameraTransitionOptions,
    ): List<ValueAnimator> {
        val animators = mutableListOf<ValueAnimator>()

        val animationDuration = LINEAR_ANIMATION_DURATION
            .coerceAtMost(transitionOptions.maxDuration)

        cameraOptions.center?.let { center ->
            val centerAnimator = cameraPlugin.createCenterAnimator(
                CameraAnimatorOptions.cameraAnimatorOptions(center) {
                    owner(NAVIGATION_CAMERA_OWNER)
                },
            ) {
                duration = animationDuration
                interpolator = LINEAR_INTERPOLATOR
            }
            animators.add(centerAnimator)
        }

        cameraOptions.zoom?.let { zoom ->
            val zoomAnimator = cameraPlugin.createZoomAnimator(
                CameraAnimatorOptions.cameraAnimatorOptions(zoom) {
                    owner(NAVIGATION_CAMERA_OWNER)
                },
            ) {
                duration = animationDuration
                interpolator = LINEAR_INTERPOLATOR
            }
            animators.add(zoomAnimator)
        }

        cameraOptions.bearing?.let { bearing ->
            val bearingShortestRotation = normalizeBearing(mapboxMap.cameraState.bearing, bearing)
            val bearingAnimator = cameraPlugin.createBearingAnimator(
                CameraAnimatorOptions.cameraAnimatorOptions(bearingShortestRotation) {
                    owner(NAVIGATION_CAMERA_OWNER)
                },
            ) {
                duration = animationDuration
                interpolator = LINEAR_INTERPOLATOR
            }
            animators.add(bearingAnimator)
        }

        cameraOptions.pitch?.let { pitch ->
            val pitchAnimator = cameraPlugin.createPitchAnimator(
                CameraAnimatorOptions.cameraAnimatorOptions(pitch) {
                    owner(NAVIGATION_CAMERA_OWNER)
                },
            ) {
                duration = animationDuration
                interpolator = LINEAR_INTERPOLATOR
            }
            animators.add(pitchAnimator)
        }

        cameraOptions.padding?.let { padding ->
            val paddingAnimator = cameraPlugin.createPaddingAnimator(
                CameraAnimatorOptions.cameraAnimatorOptions(padding) {
                    owner(NAVIGATION_CAMERA_OWNER)
                },
            ) {
                duration = animationDuration
                interpolator = LINEAR_INTERPOLATOR
            }
            animators.add(paddingAnimator)
        }

        return animators
    }

    private companion object {
        private const val LINEAR_ANIMATION_DURATION = 1000L
        private val LINEAR_INTERPOLATOR = PathInterpolatorCompat.create(0f, 0f, 1f, 1f)
    }
}
