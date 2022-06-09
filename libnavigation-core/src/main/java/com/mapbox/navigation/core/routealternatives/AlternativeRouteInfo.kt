package com.mapbox.navigation.core.routealternatives

import com.mapbox.navigation.core.directions.session.RoutesObserver

/**
 * Valid only until [RoutesObserver] or [NavigationRouteAlternativesObserver] fires again.
 *
 * @param distance distance (based on the referenced route)
 * @param duration duration (based on the referenced route)
 */
class AlternativeRouteInfo internal constructor(
    val distance: Double,
    val duration: Double,
) {
    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AlternativeRouteInfo

        if (distance != other.distance) return false
        if (duration != other.duration) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = distance.hashCode()
        result = 31 * result + duration.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "AlternativeRouteInfo(distance=$distance, duration=$duration)"
    }
}
