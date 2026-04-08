package com.mapbox.navigation.ui.maps.camera.data

import com.mapbox.maps.CameraOptions
import com.mapbox.navigation.ui.maps.internal.camera.ALMOST_EQUAL_BEARING_DEGREES
import com.mapbox.navigation.ui.maps.internal.camera.ALMOST_EQUAL_LOCATION_DEGREES
import kotlin.math.abs

private fun shortestAngle(a: Double, b: Double): Double {
    val diff = ((b - a + 180.0) % 360.0 + 360.0) % 360.0 - 180.0
    return abs(diff)
}

/**
 * Returns true if this camera position is almost equal to [other] — i.e., the car is at standstill
 * and no meaningful camera update is needed.
 *
 * A `null` center, bearing, or missing fields on either side causes the comparison to return false
 * (i.e., the positions are treated as different), so updates are not skipped.
 */
internal fun CameraOptions.isStandstill(other: CameraOptions): Boolean {
    val c1 = center
    val c2 = other.center
    if (c1 != null && c2 != null) {
        if (abs(c1.latitude() - c2.latitude()) > ALMOST_EQUAL_LOCATION_DEGREES) return false
        if (abs(c1.longitude() - c2.longitude()) > ALMOST_EQUAL_LOCATION_DEGREES) return false
    } else if (c1 != c2) {
        return false
    }

    val b1 = bearing
    val b2 = other.bearing
    if (b1 != null && b2 != null) {
        if (shortestAngle(b1, b2) > ALMOST_EQUAL_BEARING_DEGREES) return false
    } else if (b1 != b2) {
        return false
    }

    if (this.padding != other.padding) return false
    if (this.anchor != other.anchor) return false
    if (this.zoom != other.zoom) return false
    if (this.pitch != other.pitch) return false
    return true
}
