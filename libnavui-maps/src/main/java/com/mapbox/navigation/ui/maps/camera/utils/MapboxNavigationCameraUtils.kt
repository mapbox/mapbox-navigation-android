package com.mapbox.navigation.ui.maps.camera.utils

import android.animation.Animator
import android.animation.AnimatorSet
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.plugin.animation.animator.CameraAnimator
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

/**
 * Takes the longest animation in the set (including delay an duration) and scales the duration down to match the duration constraint if it's exceeded.
 * All other animations are scaled by the same factor which allows to mostly keep the same composition and "feel" of the animation set while shortening its duration.
 */
internal fun AnimatorSet.constraintDurationTo(maxDuration: Long): AnimatorSet {
    childAnimations.maxByOrNull { it.startDelay + it.duration }?.let {
        val longestExecutionTime = it.startDelay + it.duration
        if (longestExecutionTime > maxDuration) {
            val factor = maxDuration / (longestExecutionTime).toDouble()
            childAnimations.forEach { animator ->
                animator.startDelay = (animator.startDelay * factor).toLong()
                animator.duration = (animator.duration * factor).toLong()
            }
        }
    }
    return this
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
    targetZL: Double
): Double {
    return hypot(
        mapboxMap.project(currentPoint, targetZL).x - mapboxMap.project(targetPoint, targetZL).x,
        mapboxMap.project(targetPoint, targetZL).y - mapboxMap.project(targetPoint, targetZL).y
    )
}

internal fun screenDistanceFromMapCenterToTarget(
    mapboxMap: MapboxMap,
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
