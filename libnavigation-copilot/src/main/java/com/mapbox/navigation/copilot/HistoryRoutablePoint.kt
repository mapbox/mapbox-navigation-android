package com.mapbox.navigation.copilot

import androidx.annotation.Keep
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * HistoryRoutablePoint.
 *
 * @property coordinates of the search point
 */
@Keep
@ExperimentalPreviewMapboxNavigationAPI
class HistoryRoutablePoint(
    val coordinates: HistoryPoint,
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as HistoryRoutablePoint

        return coordinates == other.coordinates
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        return coordinates.hashCode()
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "HistoryRoutablePoint(coordinates=$coordinates)"
    }
}
