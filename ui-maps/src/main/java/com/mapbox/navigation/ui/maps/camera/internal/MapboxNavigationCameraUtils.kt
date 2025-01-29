package com.mapbox.navigation.ui.maps.camera.internal

import android.animation.AnimatorSet
import androidx.annotation.RestrictTo
import com.mapbox.geojson.Point
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Takes the longest animation in the set (including delay an duration) and scales the duration down to match the duration constraint if it's exceeded.
 * All other animations are scaled by the same factor which allows to mostly keep the same composition and "feel" of the animation set while shortening its duration.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun AnimatorSet.constraintDurationTo(maxDuration: Long): AnimatorSet {
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

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun NavigationCamera.jumpToCameraCenterInternal(center: Point?) {
    jumpToCameraCenter(center)
}

/**
 * Returns a bearing change using the shortest path.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun normalizeBearing(currentBearing: Double, targetBearing: Double): Double {
    /*
    rounding is a workaround for https://github.com/mapbox/mapbox-maps-android/issues/274
    it prevents wrapping to 360 degrees for very small, negative numbers and prevents the camera
    from spinning around unintentionally
    */
    return (currentBearing + shortestRotation(currentBearing, targetBearing)).roundTo(6)
}

private fun shortestRotation(from: Double, to: Double): Double {
    return (to - from + 540) % 360 - 180
}

private fun Double.roundTo(numFractionDigits: Int): Double {
    val factor = 10.0.pow(numFractionDigits.toDouble())
    return (this * factor).roundToInt() / factor
}
