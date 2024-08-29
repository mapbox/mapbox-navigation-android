package com.mapbox.navigation.copilot

import androidx.annotation.Keep
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.internal.utils.safeCompareTo

/**
 * HistoryPoint.
 *
 * @property latitude coordinate of the search point
 * @property longitude coordinate of the search point
 */
@Keep
@ExperimentalPreviewMapboxNavigationAPI
class HistoryPoint(
    val latitude: Double,
    val longitude: Double,
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as HistoryPoint

        if (!latitude.safeCompareTo(other.latitude)) return false
        return longitude.safeCompareTo(other.longitude)
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = latitude.hashCode()
        result = 31 * result + longitude.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "HistoryPoint(latitude=$latitude, longitude=$longitude)"
    }
}
