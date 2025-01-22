package com.mapbox.navigation.core.routealternatives

import com.mapbox.geojson.Point
import com.mapbox.navigation.core.directions.session.RoutesObserver

/**
 * Valid only until [RoutesObserver] or [NavigationRouteAlternativesObserver] fires again.
 *
 * @param location intersection point between the primary and alternative route
 * @param geometryIndexInRoute geometry index of the intersection point (based on referenced route's geometry)
 * @param geometryIndexInLeg geometry index of the intersection point (based on referenced route leg's geometry)
 * @param legIndex index of the leg where the intersection point is found (based on the referenced route)
 */
class AlternativeRouteIntersection internal constructor(
    val location: Point,
    val geometryIndexInRoute: Int,
    val geometryIndexInLeg: Int,
    val legIndex: Int,
) {
    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AlternativeRouteIntersection

        if (location != other.location) return false
        if (geometryIndexInRoute != other.geometryIndexInRoute) return false
        if (geometryIndexInLeg != other.geometryIndexInLeg) return false
        if (legIndex != other.legIndex) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = location.hashCode()
        result = 31 * result + geometryIndexInRoute
        result = 31 * result + geometryIndexInLeg
        result = 31 * result + legIndex
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "AlternativeRouteIntersection(" +
            "location=$location, " +
            "geometryIndexInRoute=$geometryIndexInRoute, " +
            "geometryIndexInLeg=$geometryIndexInLeg, " +
            "legIndex=$legIndex" +
            ")"
    }
}
