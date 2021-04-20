package com.mapbox.navigation.ui.maps.camera.utils

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

private fun shortestRotation(from: Double, to: Double): Double {
    return (to - from + 540) % 360 - 180
}

private fun Double.roundTo(numFractionDigits: Int): Double {
    val factor = 10.0.pow(numFractionDigits.toDouble())
    return (this * factor).roundToInt() / factor
}
