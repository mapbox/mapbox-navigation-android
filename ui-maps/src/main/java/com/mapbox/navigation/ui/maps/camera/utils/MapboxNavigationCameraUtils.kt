package com.mapbox.navigation.ui.maps.camera.utils

import android.animation.Animator
import android.animation.AnimatorSet
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.plugin.animation.CameraAnimationsPlugin
import com.mapbox.maps.plugin.animation.CameraAnimatorsFactory
import com.mapbox.maps.plugin.animation.animator.CameraAnimator
import com.mapbox.maps.plugin.animation.getCameraAnimatorsFactory
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Returns a bearing change using the shortest path.
 */
internal fun normalizeBearing(currentBearing: Double, targetBearing: Double): Double {
    /*
    rounding is a workaround for https://github.com/mapbox/mapbox-maps-android/issues/274
    it prevents wrapping to 360 degrees for very small, negative numbers and prevents the camera
    from spinning around unintentionally
    */
    return (currentBearing + shortestRotation(currentBearing, targetBearing)).roundTo(6)
}

internal fun normalizeProjection(projectedDistance: Double): Double {
    return ((ln((projectedDistance / 1000.0) + 0.24) + 2.1) * 1000.0)
}

private fun shortestRotation(from: Double, to: Double): Double {
    return (to - from + 540) % 360 - 180
}

private fun Double.roundTo(numFractionDigits: Int): Double {
    val factor = 10.0.pow(numFractionDigits.toDouble())
    return (this * factor).roundToInt() / factor
}

internal fun createAnimatorSet(animators: List<Animator>) = AnimatorSet().apply {
    playTogether(*(animators.toTypedArray()))
}

internal fun createAnimatorSetWith(animators: Array<CameraAnimator<*>>) = AnimatorSet().apply {
    playTogether(*(animators))
}

internal fun projectedDistance(
    mapboxMap: MapboxMap,
    currentPoint: Point,
    targetPoint: Point,
    targetZL: Double,
): Double {
    return hypot(
        mapboxMap.project(currentPoint, targetZL).x - mapboxMap.project(targetPoint, targetZL).x,
        mapboxMap.project(targetPoint, targetZL).y - mapboxMap.project(targetPoint, targetZL).y,
    )
}

internal fun screenDistanceFromMapCenterToTarget(
    mapboxMap: MapboxMap,
    currentCenter: Point,
    targetCenter: Point,
): Double {
    val currentCenterScreenCoordinate = mapboxMap.pixelForCoordinate(currentCenter)
    val locationScreenCoordinate = mapboxMap.pixelForCoordinate(targetCenter)
    return hypot(
        currentCenterScreenCoordinate.x - locationScreenCoordinate.x,
        currentCenterScreenCoordinate.y - locationScreenCoordinate.y,
    )
}

internal fun CameraAnimationsPlugin.getAnimatorsFactory(): CameraAnimatorsFactory =
    getCameraAnimatorsFactory()
