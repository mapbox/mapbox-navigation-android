package com.mapbox.navigation.ui.maps.route.line.model

import com.mapbox.geojson.Point
import com.mapbox.navigation.base.internal.utils.safeCompareTo

/**
 * @param point the upcoming, not yet visited point on the route
 * @param distanceRemaining distance remaining from the upcoming point
 */
class RouteLineDistancesIndex(val point: Point, val distanceRemaining: Double) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RouteLineDistancesIndex

        if (point != other.point) return false
        return distanceRemaining.safeCompareTo(other.distanceRemaining)
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = point.hashCode()
        result = 31 * result + distanceRemaining.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "RouteLineDistancesIndex(point=$point, distanceRemaining=$distanceRemaining)"
    }
}
