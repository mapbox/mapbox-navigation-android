package com.mapbox.navigation.base.trip.model.roadobject

/**
 * Abstract class that serves as a base for all road objects.
 *
 * Available road object types are:
 * - [TunnelEntrance]
 * - [TunnelExit]
 * - [CountryBorderCrossing]
 * - [TollCollection]
 * - [RestStop]
 * - [RestrictedAreaEntrance]
 * - [RestrictedAreaExit]
 * - [BridgeEntrance]
 * - [BridgeExit]
 * - [Incident]
 * - [Custom]
 *
 * @param objectType constant describing the object type, see [RoadObjectType].
 * @param distanceFromStartOfRoute distance to this object since the start of the route.
 * Will be null for road objects returned by Electronic Horizon.
 * @param objectGeometry geometry details of the object.
 */
abstract class RoadObject(
    val objectType: Int,
    distanceFromStartOfRoute: Double?,
    val objectGeometry: RoadObjectGeometry,
) {
    /**
     * Distance to this object since the start of the route.
     * Will be null for road objects returned by Electronic Horizon.
     */
    val distanceFromStartOfRoute: Double? = distanceFromStartOfRoute?.let {
        if (it >= 0) it else null
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RoadObject

        if (objectType != other.objectType) return false
        if (distanceFromStartOfRoute != other.distanceFromStartOfRoute) return false
        if (objectGeometry != other.objectGeometry) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = objectType.hashCode()
        result = 31 * result + distanceFromStartOfRoute.hashCode()
        result = 31 * result + (objectGeometry.hashCode())
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "RoadObject(" +
            "type=$objectType, " +
            "distanceFromStartOfRoute=$distanceFromStartOfRoute, " +
            "objectGeometry=$objectGeometry" +
            ")"
    }
}
