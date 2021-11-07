package com.mapbox.navigation.base.road.model

import com.mapbox.api.directions.v5.models.MapboxShield

/**
 * Object that holds road properties
 * @property name of the road if available otherwise null
 * @property shieldUrl url for the route shield if available otherwise null
 * @property shieldName name of the route shield if available otherwise null
 * @property mapboxShield mapbox designed shield if available otherwise null
 */
class Road internal constructor(
    val name: String? = null,
    val shieldUrl: String? = null,
    val shieldName: String? = null,
    val mapboxShield: List<MapboxShield>? = null
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Road

        if (name != other.name) return false
        if (shieldUrl != other.shieldUrl) return false
        if (shieldName != other.shieldName) return false
        if (mapboxShield != other.mapboxShield) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = name?.hashCode() ?: 0
        result = 31 * result + (shieldUrl?.hashCode() ?: 0)
        result = 31 * result + (shieldName?.hashCode() ?: 0)
        result = 31 * result + (mapboxShield?.hashCode() ?: 0)
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "Road(" +
            "name=$name, " +
            "shieldUrl=$shieldUrl, " +
            "shieldName=$shieldName, " +
            "mapboxShield=$mapboxShield" +
            ")"
    }
}
