package com.mapbox.navigation.base.road.model

import com.mapbox.api.directions.v5.models.MapboxShield

/**
 * Object that holds road components
 * @property text of the road
 * @property shield mapbox designed shield if available otherwise null
 * @property imageBaseUrl url for the route shield if available otherwise null
 */
class RoadComponent internal constructor(
    val text: String,
    val shield: MapboxShield? = null,
    val imageBaseUrl: String? = null
) {
    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RoadComponent

        if (text != other.text) return false
        if (shield != other.shield) return false
        if (imageBaseUrl != other.imageBaseUrl) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = text.hashCode()
        result = 31 * result + (shield?.hashCode() ?: 0)
        result = 31 * result + (imageBaseUrl?.hashCode() ?: 0)
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "RoadComponent(text='$text', shield=$shield, imageBaseUrl=$imageBaseUrl)"
    }
}
