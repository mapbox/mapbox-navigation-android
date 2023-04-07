package com.mapbox.navigation.base.route

import androidx.annotation.StringDef
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.route.LegWaypoint.Type

/**
 * The class contains information about a waypoint which breaks the route into legs (not the silent one).
 * @param location waypoint location provided in the route request via `coordinates` parameter.
 * @param name waypoint name provided in the route request via `waypoint_names` parameter.
 * @param target waypoint target provided in the route request via `waypoint_targets` parameter.
 * @param type waypoint type (see [Type]]).
 */
class LegWaypoint internal constructor(
    val location: Point,
    val name: String,
    val target: Point?,
    @Type val type: String,
) {

    companion object {
        /**
         * Regular waypoint type: a regular user-added stop along the route.
         */
        const val REGULAR = "REGULAR"

        /**
         * EV charging station waypoint type: the waypoint that was added by server for an EV route.
         */
        const val EV_CHARGING_ADDED = "EV_CHARGING_ADDED"
    }

    /**
     * Leg waypoint type
     */
    @Target(
        AnnotationTarget.PROPERTY,
        AnnotationTarget.VALUE_PARAMETER,
        AnnotationTarget.FUNCTION,
        AnnotationTarget.TYPE
    )
    @Retention(AnnotationRetention.BINARY)
    @StringDef(REGULAR, EV_CHARGING_ADDED)
    annotation class Type

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LegWaypoint

        if (location != other.location) return false
        if (name != other.name) return false
        if (target != other.target) return false
        if (type != other.type) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = location.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + target.hashCode()
        result = 31 * result + type.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "LegWaypoint(location=$location, name='$name', target=$target, type='$type')"
    }
}
