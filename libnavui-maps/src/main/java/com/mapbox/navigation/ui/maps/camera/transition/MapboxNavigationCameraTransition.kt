package com.mapbox.navigation.ui.maps.camera.transition

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import androidx.core.view.animation.PathInterpolatorCompat
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.plugin.animation.CameraAnimationsPlugin
import com.mapbox.maps.plugin.animation.CameraAnimatorOptions
import com.mapbox.navigation.ui.maps.camera.utils.shortestRotation
import kotlin.math.abs
import kotlin.math.hypot

private const val LINEAR_ANIMATION_DURATION = 1000L
private const val MAXIMUM_LOW_TO_HIGH_DURATION = 3000L
private val LINEAR_INTERPOLATOR = PathInterpolatorCompat.create(0f, 0f, 1f, 1f)
private val SLOW_OUT_SLOW_IN_INTERPOLATOR = PathInterpolatorCompat.create(0.4f, 0f, 0.4f, 1f)

/**
 * Helper class that provides default implementation of [NavigationCameraTransition] generators.
 */
class MapboxNavigationCameraTransition(
    private val mapboxMap: MapboxMap,
    private val cameraPlugin: CameraAnimationsPlugin
) : NavigationCameraTransition {

    override fun transitionFromLowZoomToHighZoom(
        cameraOptions: CameraOptions
    ): AnimatorSet {
        val animators = mutableListOf<ValueAnimator>()
        val currentMapCameraOptions = mapboxMap.getCameraOptions(null)

        var centerDuration = 0L
        cameraOptions.center?.let { center ->
            val screenDistanceFromMapCenterToLocation = screenDistanceFromMapCenterToTarget(
                currentCenter = currentMapCameraOptions.center ?: center,
                targetCenter = center
            )

            val centerAnimationRate = 300.0
            centerDuration = (
                (screenDistanceFromMapCenterToLocation / centerAnimationRate) * 1000.0
                ).toLong()
                .coerceAtMost(MAXIMUM_LOW_TO_HIGH_DURATION)
            val centerAnimator = cameraPlugin.createCenterAnimator(
                CameraAnimatorOptions.cameraAnimatorOptions(center)
            ) {
                duration = centerDuration
                interpolator = SLOW_OUT_SLOW_IN_INTERPOLATOR
            }
            animators.add(centerAnimator)
        }

        var zoomDelay = 0.0
        var zoomDuration = 0L
        cameraOptions.zoom?.let { zoom ->
            val currentMapCameraZoom = currentMapCameraOptions.zoom
            val zoomDelta = currentMapCameraZoom?.let {
                abs(zoom - it)
            } ?: zoom

            val zoomAnimationRate = 2.0
            zoomDelay = centerDuration * 0.3
            zoomDuration = ((zoomDelta / zoomAnimationRate) * 1000.0).toLong()
                .coerceAtMost(MAXIMUM_LOW_TO_HIGH_DURATION)

            val zoomAnimator = cameraPlugin.createZoomAnimator(
                CameraAnimatorOptions.cameraAnimatorOptions(zoom)
            ) {
                startDelay = zoomDelay.toLong()
                duration = zoomDuration
                interpolator = SLOW_OUT_SLOW_IN_INTERPOLATOR
            }
            animators.add(zoomAnimator)
        }

        cameraOptions.bearing?.let { bearing ->
            var bearingShortestRotation = bearing
            currentMapCameraOptions.bearing?.let {
                bearingShortestRotation = it + shortestRotation(it, bearing)
            }
            val bearingDuration = 1800.0
            val bearingDelay = (zoomDelay + zoomDuration - bearingDuration).coerceAtLeast(0.0)
            val bearingAnimator = cameraPlugin.createBearingAnimator(
                CameraAnimatorOptions.cameraAnimatorOptions(bearingShortestRotation)
            ) {
                startDelay = bearingDelay.toLong()
                duration = bearingDuration.toLong()
                interpolator = SLOW_OUT_SLOW_IN_INTERPOLATOR
            }
            animators.add(bearingAnimator)
        }

        val pitchAndPaddingDuration = 1200.0
        val pitchAndPaddingDelay = (
            zoomDelay + zoomDuration - pitchAndPaddingDuration + 100
            )
            .coerceAtLeast(0.0)
        cameraOptions.pitch?.let { pitch ->
            val pitchAnimator = cameraPlugin.createPitchAnimator(
                CameraAnimatorOptions.cameraAnimatorOptions(pitch)
            ) {
                startDelay = pitchAndPaddingDelay.toLong()
                duration = pitchAndPaddingDuration.toLong()
                interpolator = SLOW_OUT_SLOW_IN_INTERPOLATOR
            }
            animators.add(pitchAnimator)
        }

        cameraOptions.padding?.let { padding ->
            val paddingAnimator = cameraPlugin.createPaddingAnimator(
                CameraAnimatorOptions.cameraAnimatorOptions(padding)
            ) {
                startDelay = pitchAndPaddingDelay.toLong()
                duration = pitchAndPaddingDuration.toLong()
                interpolator = SLOW_OUT_SLOW_IN_INTERPOLATOR
            }
            animators.add(paddingAnimator)
        }

        return AnimatorSet().apply {
            playTogether(*(animators.toTypedArray()))
        }
    }

    override fun transitionFromHighZoomToLowZoom(
        cameraOptions: CameraOptions
    ): AnimatorSet {
        val animators = mutableListOf<ValueAnimator>()
        val currentMapCameraOptions = mapboxMap.getCameraOptions(null)

        cameraOptions.center?.let { center ->
            val centerAnimator = cameraPlugin.createCenterAnimator(
                CameraAnimatorOptions.cameraAnimatorOptions(center)
            ) {
                startDelay = 800
                duration = 1000
                interpolator = SLOW_OUT_SLOW_IN_INTERPOLATOR
            }
            animators.add(centerAnimator)
        }

        cameraOptions.zoom?.let { zoom ->
            val zoomAnimator = cameraPlugin.createZoomAnimator(
                CameraAnimatorOptions.cameraAnimatorOptions(zoom)
            ) {
                startDelay = 0
                duration = 1800
                interpolator = SLOW_OUT_SLOW_IN_INTERPOLATOR
            }
            animators.add(zoomAnimator)
        }

        cameraOptions.bearing?.let { bearing ->
            var bearingShortestRotation = bearing
            currentMapCameraOptions.bearing?.let {
                bearingShortestRotation = it + shortestRotation(it, bearing)
            }
            val bearingAnimator = cameraPlugin.createBearingAnimator(
                CameraAnimatorOptions.cameraAnimatorOptions(bearingShortestRotation)
            ) {
                startDelay = 600
                duration = 1200
                interpolator = SLOW_OUT_SLOW_IN_INTERPOLATOR
            }
            animators.add(bearingAnimator)
        }

        cameraOptions.pitch?.let { pitch ->
            val pitchAnimator = cameraPlugin.createPitchAnimator(
                CameraAnimatorOptions.cameraAnimatorOptions(pitch)
            ) {
                startDelay = 0
                duration = 1000
                interpolator = SLOW_OUT_SLOW_IN_INTERPOLATOR
            }
            animators.add(pitchAnimator)
        }

        cameraOptions.padding?.let { padding ->
            val paddingAnimator = cameraPlugin.createPaddingAnimator(
                CameraAnimatorOptions.cameraAnimatorOptions(padding)
            ) {
                startDelay = 0
                duration = 1200
                interpolator = SLOW_OUT_SLOW_IN_INTERPOLATOR
            }
            animators.add(paddingAnimator)
        }

        return AnimatorSet().apply {
            playTogether(*(animators.toTypedArray()))
        }
    }

    override fun transitionLinear(
        cameraOptions: CameraOptions
    ): AnimatorSet {
        val animators = mutableListOf<ValueAnimator>()
        val currentMapCamera = mapboxMap.getCameraOptions(null)

        cameraOptions.center?.let { center ->
            val centerAnimator = cameraPlugin.createCenterAnimator(
                CameraAnimatorOptions.cameraAnimatorOptions(center)
            ) {
                duration = LINEAR_ANIMATION_DURATION
                interpolator = LINEAR_INTERPOLATOR
            }
            animators.add(centerAnimator)
        }

        cameraOptions.zoom?.let { zoom ->
            val zoomAnimator = cameraPlugin.createZoomAnimator(
                CameraAnimatorOptions.cameraAnimatorOptions(zoom)
            ) {
                duration = LINEAR_ANIMATION_DURATION
                interpolator = LINEAR_INTERPOLATOR
            }
            animators.add(zoomAnimator)
        }

        cameraOptions.bearing?.let { bearing ->
            var bearingShortestRotation = bearing
            currentMapCamera.bearing?.let {
                bearingShortestRotation = it + shortestRotation(it, bearing)
            }
            val bearingAnimator = cameraPlugin.createBearingAnimator(
                CameraAnimatorOptions.cameraAnimatorOptions(bearingShortestRotation)
            ) {
                duration = LINEAR_ANIMATION_DURATION
                interpolator = LINEAR_INTERPOLATOR
            }
            animators.add(bearingAnimator)
        }

        cameraOptions.pitch?.let { pitch ->
            val pitchAnimator = cameraPlugin.createPitchAnimator(
                CameraAnimatorOptions.cameraAnimatorOptions(pitch)
            ) {
                duration = LINEAR_ANIMATION_DURATION
                interpolator = LINEAR_INTERPOLATOR
            }
            animators.add(pitchAnimator)
        }

        cameraOptions.padding?.let { padding ->
            val paddingAnimator = cameraPlugin.createPaddingAnimator(
                CameraAnimatorOptions.cameraAnimatorOptions(padding)
            ) {
                duration = LINEAR_ANIMATION_DURATION
                interpolator = LINEAR_INTERPOLATOR
            }
            animators.add(paddingAnimator)
        }

        return AnimatorSet().apply {
            playTogether(*(animators.toTypedArray()))
        }
    }

    private fun screenDistanceFromMapCenterToTarget(
        currentCenter: Point,
        targetCenter: Point
    ): Double {
        val currentCenterScreenCoordinate = mapboxMap.pixelForCoordinate(currentCenter)
        val locationScreenCoordinate = mapboxMap.pixelForCoordinate(targetCenter)
        return hypot(
            currentCenterScreenCoordinate.x - locationScreenCoordinate.x,
            currentCenterScreenCoordinate.y - locationScreenCoordinate.y
        )
    }
}
