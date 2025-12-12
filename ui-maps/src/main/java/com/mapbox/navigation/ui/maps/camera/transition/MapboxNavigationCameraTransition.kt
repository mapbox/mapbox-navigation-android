package com.mapbox.navigation.ui.maps.camera.transition

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import androidx.annotation.VisibleForTesting
import androidx.core.view.animation.PathInterpolatorCompat
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.plugin.animation.CameraAnimationsPlugin
import com.mapbox.maps.plugin.animation.CameraAnimatorOptions
import com.mapbox.maps.plugin.animation.CameraAnimatorsFactory
import com.mapbox.navigation.ui.maps.camera.NavigationCamera.Companion.NAVIGATION_CAMERA_OWNER
import com.mapbox.navigation.ui.maps.camera.utils.createAnimatorSet
import com.mapbox.navigation.ui.maps.camera.utils.createAnimatorSetWith
import com.mapbox.navigation.ui.maps.camera.utils.getAnimatorsFactory
import com.mapbox.navigation.ui.maps.camera.utils.normalizeProjection
import com.mapbox.navigation.ui.maps.camera.utils.projectedDistance
import com.mapbox.navigation.ui.maps.internal.camera.constraintDurationTo
import com.mapbox.navigation.utils.internal.ifNonNull
import kotlin.math.abs
import kotlin.math.min

private val SLOW_OUT_SLOW_IN_INTERPOLATOR = PathInterpolatorCompat.create(0.4f, 0f, 0.4f, 1f)

/**
 * Helper class that provides default implementation of [NavigationCameraTransition] generators.
 */
class MapboxNavigationCameraTransition @VisibleForTesting internal constructor(
    private val mapboxMap: MapboxMap,
    private val cameraPlugin: CameraAnimationsPlugin,
    private val updateFrame: DefaultSimplifiedUpdateFrameTransitionProvider,
) : NavigationCameraTransition {

    constructor(
        mapboxMap: MapboxMap,
        cameraPlugin: CameraAnimationsPlugin,
    ) : this(
        mapboxMap,
        cameraPlugin,
        DefaultSimplifiedUpdateFrameTransitionProvider(cameraPlugin),
    )

    override fun transitionFromLowZoomToHighZoom(
        cameraOptions: CameraOptions,
        transitionOptions: NavigationCameraTransitionOptions,
    ): AnimatorSet {
        val cameraFactory = cameraPlugin.getAnimatorsFactory()

        return flyFromLowZoomToHighZoom(cameraOptions, cameraFactory, transitionOptions)
    }

    override fun transitionFromHighZoomToLowZoom(
        cameraOptions: CameraOptions,
        transitionOptions: NavigationCameraTransitionOptions,
    ): AnimatorSet {
        val animators = mutableListOf<ValueAnimator>()
        cameraOptions.center?.let { center ->
            val centerAnimator = cameraPlugin.createCenterAnimator(
                CameraAnimatorOptions.cameraAnimatorOptions(center) {
                    owner(NAVIGATION_CAMERA_OWNER)
                },
                useShortestPath = false,
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
                },
            ) {
                startDelay = 0
                duration = 1800
                interpolator = SLOW_OUT_SLOW_IN_INTERPOLATOR
            }
            animators.add(zoomAnimator)
        }

        cameraOptions.bearing?.let { bearing ->
            val bearingAnimator = cameraPlugin.createBearingAnimator(
                CameraAnimatorOptions.cameraAnimatorOptions(bearing) {
                    owner(NAVIGATION_CAMERA_OWNER)
                },
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
                },
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
                },
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
        transitionOptions: NavigationCameraTransitionOptions,
    ): AnimatorSet {
        return createAnimatorSet(updateFrame.updateFrame(cameraOptions, transitionOptions))
    }

    private fun flyFromLowZoomToHighZoom(
        cameraOptions: CameraOptions,
        factory: CameraAnimatorsFactory,
        transitionOptions: NavigationCameraTransitionOptions,
    ): AnimatorSet {
        var duration = 0L
        val currentMapCameraState = mapboxMap.cameraState
        val currentPoint = currentMapCameraState.center
        val currentZL = currentMapCameraState.zoom
        val targetCenter = cameraOptions.center
        val targetZoom = cameraOptions.zoom
        val animators = factory.getFlyTo(
            cameraOptions = cameraOptions,
            owner = NAVIGATION_CAMERA_OWNER,
        )
        ifNonNull(targetCenter, targetZoom) { targetPoint, targetZL ->
            val projection = projectedDistance(
                mapboxMap = mapboxMap,
                currentPoint = currentPoint,
                targetPoint = targetPoint,
                targetZL = targetZL,
            )
            val zoomDelta = abs(currentZL - targetZL) * 80
            duration = normalizeProjection(projection + zoomDelta).toLong()
        }
        duration = min(duration, transitionOptions.maxDuration)
        return createAnimatorSetWith(animators)
            .setDuration(duration)
    }
}
