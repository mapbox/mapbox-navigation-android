package com.mapbox.navigation.core.utils

import com.mapbox.turf.TurfMeasurement

/**
 * Normalize a bearing to be within the range of [0..360).
 *
 * Useful to normalize value from [TurfMeasurement.bearing].
 */
internal fun normalizeBearing(
    angle: Double,
): Double {
    return (angle + 360) % 360
}
