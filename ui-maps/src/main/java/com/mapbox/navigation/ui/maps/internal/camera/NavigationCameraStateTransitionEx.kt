package com.mapbox.navigation.ui.maps.internal.camera

import android.animation.AnimatorSet
import androidx.annotation.RestrictTo
import com.mapbox.geojson.Point
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

/**
 * Duration, in milliseconds, of a points overview transition that moves the camera from
 * [currentCenter] to [targetCenter]. The duration grows logarithmically with the distance between
 * the two, is clamped to a fixed range, and is finally capped by [maxDuration].
 *
 * A null [targetCenter] means the distance is unknown, in which case the longest duration is used.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun pointsOverviewTransitionDuration(
    currentCenter: Point,
    targetCenter: Point?,
    maxDuration: Long,
): Long {
    val duration = if (targetCenter != null) {
        val distance = TurfMeasurement.distance(targetCenter, currentCenter)
        val duration = ANIMATION_COEFFICIENT_1 * ln(distance) + ANIMATION_COEFFICIENT_2
        duration.roundToLong().coerceIn(ANIMATION_DURATION_MIN, ANIMATION_DURATION_MAX)
    } else {
        ANIMATION_DURATION_MAX
    }
    return min(duration, maxDuration)
}

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
    val duration = pointsOverviewTransitionDuration(
        currentPoint,
        targetCenter,
        transitionOptions.maxDuration,
    )
    return AnimatorSet().apply {
        playTogether(*(animators))
        setDuration(duration)
    }
}
