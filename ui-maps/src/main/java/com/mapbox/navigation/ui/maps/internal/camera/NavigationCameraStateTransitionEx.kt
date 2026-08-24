package com.mapbox.navigation.ui.maps.internal.camera

import android.animation.AnimatorSet
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.plugin.animation.CameraAnimationsPlugin
import com.mapbox.maps.plugin.animation.getCameraAnimatorsFactory
import com.mapbox.navigation.ui.maps.camera.NavigationCamera.Companion.NAVIGATION_CAMERA_OWNER
import com.mapbox.navigation.ui.maps.camera.transition.NavigationCameraTransitionOptions
import com.mapbox.turf.TurfMeasurement
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.roundToLong

private const val ANIMATION_DURATION_MIN = 1500L
private const val ANIMATION_DURATION_MAX = 4000L
private const val ANIMATION_COEFFICIENT_1 = 500
private const val ANIMATION_COEFFICIENT_2 = 200

internal fun transitionToPointsOverviewInternal(
    cameraPlugin: CameraAnimationsPlugin,
    mapboxMap: MapboxMap,
    cameraOptions: CameraOptions,
    transitionOptions: NavigationCameraTransitionOptions,
): AnimatorSet {
    val currentMapCameraState = mapboxMap.cameraState
    val currentPoint = currentMapCameraState.center
    val targetCenter = cameraOptions.center
    val animators = cameraPlugin.getCameraAnimatorsFactory().getFlyTo(
        cameraOptions = cameraOptions,
        owner = NAVIGATION_CAMERA_OWNER,
    )
    var duration = if (targetCenter != null) {
        val distance = TurfMeasurement.distance(targetCenter, currentPoint)
        val duration = ANIMATION_COEFFICIENT_1 * ln(distance) + ANIMATION_COEFFICIENT_2
        duration.roundToLong().coerceIn(ANIMATION_DURATION_MIN, ANIMATION_DURATION_MAX)
    } else {
        ANIMATION_DURATION_MAX
    }
    duration = min(duration, transitionOptions.maxDuration)
    return AnimatorSet().apply {
        playTogether(*(animators))
        setDuration(duration)
    }
}
