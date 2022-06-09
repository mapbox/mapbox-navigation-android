package com.mapbox.navigation.ui.maps.camera.transition

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import androidx.core.view.animation.PathInterpolatorCompat
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.plugin.animation.CameraAnimationsPlugin
import com.mapbox.maps.plugin.animation.CameraAnimationsPluginImpl
import com.mapbox.maps.plugin.animation.CameraAnimatorOptions
import com.mapbox.navigation.ui.maps.camera.NavigationCamera.Companion.NAVIGATION_CAMERA_OWNER
import com.mapbox.navigation.ui.maps.camera.utils.constraintDurationTo
import com.mapbox.navigation.ui.maps.camera.utils.createAnimatorSet
import com.mapbox.navigation.ui.maps.camera.utils.createAnimatorSetWith
import com.mapbox.navigation.ui.maps.camera.utils.normalizeBearing
import com.mapbox.navigation.ui.maps.camera.utils.normalizeProjection
import com.mapbox.navigation.ui.maps.camera.utils.projectedDistance
import com.mapbox.navigation.ui.maps.camera.utils.screenDistanceFromMapCenterToTarget
import com.mapbox.navigation.utils.internal.ifNonNull
import kotlin.math.abs

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
        cameraOptions: CameraOptions,
        transitionOptions: NavigationCameraTransitionOptions
    ): AnimatorSet {
        val pluginImpl: CameraAnimationsPluginImpl? = cameraPlugin as? CameraAnimationsPluginImpl

        return ifNonNull(pluginImpl) {
            flyFromLowZoomToHighZoom(cameraOptions, it, transitionOptions)
        } ?: fromLowZoomToHighZoom(cameraOptions, transitionOptions)
    }

    override fun transitionFromHighZoomToLowZoom(
        cameraOptions: CameraOptions,
        transitionOptions: NavigationCameraTransitionOptions
    ): AnimatorSet {
        val animators = mutableListOf<ValueAnimator>()
        val currentMapCameraState = mapboxMap.cameraState

        cameraOptions.center?.let { center ->
            val centerAnimator = cameraPlugin.createCenterAnimator(
                CameraAnimatorOptions.cameraAnimatorOptions(center) {
                    owner(NAVIGATION_CAMERA_OWNER)
                }
            ) {
                startDelay = 800
                duration = 1000
                interpolator = SLOW_OUT_SLOW_IN_INTERPOLATOR
            }
            animators.add(centerAnimator)
        }

        cameraOptions.zoom?.let { zoom ->
            val zoomAnimator = cameraPlugin.createZoomAnimator(
                CameraAnimatorOptions.cameraAnimatorOptions(zoom) {
                    owner(NAVIGATION_CAMERA_OWNER)
                }
            ) {
                startDelay = 0
                duration = 1800
                interpolator = SLOW_OUT_SLOW_IN_INTERPOLATOR
            }
            animators.add(zoomAnimator)
        }

        cameraOptions.bearing?.let { bearing ->
            val bearingShortestRotation = normalizeBearing(currentMapCameraState.bearing, bearing)
            val bearingAnimator = cameraPlugin.createBearingAnimator(
                CameraAnimatorOptions.cameraAnimatorOptions(bearingShortestRotation) {
                    owner(NAVIGATION_CAMERA_OWNER)
                }
            ) {
                startDelay = 600
                duration = 1200
                interpolator = SLOW_OUT_SLOW_IN_INTERPOLATOR
            }
            animators.add(bearingAnimator)
        }

        cameraOptions.pitch?.let { pitch ->
            val pitchAnimator = cameraPlugin.createPitchAnimator(
                CameraAnimatorOptions.cameraAnimatorOptions(pitch) {
                    owner(NAVIGATION_CAMERA_OWNER)
                }
            ) {
                startDelay = 0
                duration = 1000
                interpolator = SLOW_OUT_SLOW_IN_INTERPOLATOR
            }
            animators.add(pitchAnimator)
        }

        cameraOptions.padding?.let { padding ->
            val paddingAnimator = cameraPlugin.createPaddingAnimator(
                CameraAnimatorOptions.cameraAnimatorOptions(padding) {
                    owner(NAVIGATION_CAMERA_OWNER)
                }
            ) {
                startDelay = 0
                duration = 1200
                interpolator = SLOW_OUT_SLOW_IN_INTERPOLATOR
            }
            animators.add(paddingAnimator)
        }

        return createAnimatorSet(animators).constraintDurationTo(transitionOptions.maxDuration)
    }

    override fun transitionLinear(
        cameraOptions: CameraOptions,
        transitionOptions: NavigationCameraTransitionOptions
    ): AnimatorSet {
        val animators = mutableListOf<ValueAnimator>()

        cameraOptions.center?.let { center ->
            val centerAnimator = cameraPlugin.createCenterAnimator(
                CameraAnimatorOptions.cameraAnimatorOptions(center) {
                    owner(NAVIGATION_CAMERA_OWNER)
                }
            ) {
                duration = LINEAR_ANIMATION_DURATION
                interpolator = LINEAR_INTERPOLATOR
            }
            animators.add(centerAnimator)
        }

        cameraOptions.zoom?.let { zoom ->
            val zoomAnimator = cameraPlugin.createZoomAnimator(
                CameraAnimatorOptions.cameraAnimatorOptions(zoom) {
                    owner(NAVIGATION_CAMERA_OWNER)
                }
            ) {
                duration = LINEAR_ANIMATION_DURATION
                interpolator = LINEAR_INTERPOLATOR
            }
            animators.add(zoomAnimator)
        }

        cameraOptions.bearing?.let { bearing ->
            val bearingShortestRotation = normalizeBearing(mapboxMap.cameraState.bearing, bearing)
            val bearingAnimator = cameraPlugin.createBearingAnimator(
                CameraAnimatorOptions.cameraAnimatorOptions(bearingShortestRotation) {
                    owner(NAVIGATION_CAMERA_OWNER)
                }
            ) {
                duration = LINEAR_ANIMATION_DURATION
                interpolator = LINEAR_INTERPOLATOR
            }
            animators.add(bearingAnimator)
        }

        cameraOptions.pitch?.let { pitch ->
            val pitchAnimator = cameraPlugin.createPitchAnimator(
                CameraAnimatorOptions.cameraAnimatorOptions(pitch) {
                    owner(NAVIGATION_CAMERA_OWNER)
                }
            ) {
                duration = LINEAR_ANIMATION_DURATION
                interpolator = LINEAR_INTERPOLATOR
            }
            animators.add(pitchAnimator)
        }

        cameraOptions.padding?.let { padding ->
            val paddingAnimator = cameraPlugin.createPaddingAnimator(
                CameraAnimatorOptions.cameraAnimatorOptions(padding) {
                    owner(NAVIGATION_CAMERA_OWNER)
                }
            ) {
                duration = LINEAR_ANIMATION_DURATION
                interpolator = LINEAR_INTERPOLATOR
            }
            animators.add(paddingAnimator)
        }

        return createAnimatorSet(animators).constraintDurationTo(transitionOptions.maxDuration)
    }

    private fun fromLowZoomToHighZoom(
        cameraOptions: CameraOptions,
        transitionOptions: NavigationCameraTransitionOptions
    ): AnimatorSet {
        val animators = mutableListOf<ValueAnimator>()
        val currentMapCameraState = mapboxMap.cameraState

        var centerDuration = 0L
        cameraOptions.center?.let { center ->
            val screenDistanceFromMapCenterToLocation = screenDistanceFromMapCenterToTarget(
                mapboxMap = mapboxMap,
                currentCenter = currentMapCameraState.center,
                targetCenter = center
            )

            val centerAnimationRate = 500.0
            centerDuration = (
                (screenDistanceFromMapCenterToLocation / centerAnimationRate) * 1000.0
                ).toLong()
                .coerceAtMost(MAXIMUM_LOW_TO_HIGH_DURATION)
            val centerAnimator = cameraPlugin.createCenterAnimator(
                CameraAnimatorOptions.cameraAnimatorOptions(center) {
                    owner(NAVIGATION_CAMERA_OWNER)
                }
            ) {
                duration = centerDuration
                interpolator = SLOW_OUT_SLOW_IN_INTERPOLATOR
            }
            animators.add(centerAnimator)
        }

        var zoomDelay = 0.0
        var zoomDuration = 0L
        cameraOptions.zoom?.let { zoom ->
            val currentMapCameraZoom = currentMapCameraState.zoom
            val zoomDelta = abs(zoom - currentMapCameraZoom)
            val zoomAnimationRate = 2.2
            zoomDelay = centerDuration * 0.5
            zoomDuration = ((zoomDelta / zoomAnimationRate) * 1000.0).toLong()
                .coerceAtMost(MAXIMUM_LOW_TO_HIGH_DURATION)

            val zoomAnimator = cameraPlugin.createZoomAnimator(
                CameraAnimatorOptions.cameraAnimatorOptions(zoom) {
                    owner(NAVIGATION_CAMERA_OWNER)
                }
            ) {
                startDelay = zoomDelay.toLong()
                duration = zoomDuration
                interpolator = SLOW_OUT_SLOW_IN_INTERPOLATOR
            }
            animators.add(zoomAnimator)
        }

        cameraOptions.bearing?.let { bearing ->
            val bearingShortestRotation = normalizeBearing(currentMapCameraState.bearing, bearing)
            val bearingDuration = 1800.0
            val bearingDelay = (zoomDelay + zoomDuration - bearingDuration).coerceAtLeast(0.0)
            val bearingAnimator = cameraPlugin.createBearingAnimator(
                CameraAnimatorOptions.cameraAnimatorOptions(bearingShortestRotation) {
                    owner(NAVIGATION_CAMERA_OWNER)
                }
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
                CameraAnimatorOptions.cameraAnimatorOptions(pitch) {
                    owner(NAVIGATION_CAMERA_OWNER)
                }
            ) {
                startDelay = pitchAndPaddingDelay.toLong()
                duration = pitchAndPaddingDuration.toLong()
                interpolator = SLOW_OUT_SLOW_IN_INTERPOLATOR
            }
            animators.add(pitchAnimator)
        }

        cameraOptions.padding?.let { padding ->
            val paddingAnimator = cameraPlugin.createPaddingAnimator(
                CameraAnimatorOptions.cameraAnimatorOptions(padding) {
                    owner(NAVIGATION_CAMERA_OWNER)
                }
            ) {
                startDelay = pitchAndPaddingDelay.toLong()
                duration = pitchAndPaddingDuration.toLong()
                interpolator = SLOW_OUT_SLOW_IN_INTERPOLATOR
            }
            animators.add(paddingAnimator)
        }

        return createAnimatorSet(animators).constraintDurationTo(transitionOptions.maxDuration)
    }

    private fun flyFromLowZoomToHighZoom(
        cameraOptions: CameraOptions,
        pluginImpl: CameraAnimationsPluginImpl,
        transitionOptions: NavigationCameraTransitionOptions
    ): AnimatorSet {
        var duration = 0L
        val currentMapCameraState = mapboxMap.cameraState
        val currentPoint = currentMapCameraState.center
        val currentZL = currentMapCameraState.zoom
        val targetCenter = cameraOptions.center
        val targetZoom = cameraOptions.zoom
        val animators = pluginImpl.cameraAnimationsFactory.getFlyTo(cameraOptions)
        ifNonNull(targetCenter, targetZoom) { targetPoint, targetZL ->
            val projection = projectedDistance(
                mapboxMap = mapboxMap,
                currentPoint = currentPoint,
                targetPoint = targetPoint,
                targetZL = targetZL
            )
            val zoomDelta = abs(currentZL - targetZL) * 80
            duration = normalizeProjection(projection + zoomDelta).toLong()
        }
        return createAnimatorSetWith(animators)
            .setDuration(duration)
            .constraintDurationTo(transitionOptions.maxDuration)
    }
}
