package com.mapbox.navigation.ui.maneuver.model

import com.mapbox.api.directions.v5.models.MapboxShield

/**
 * Data structure  that holds information about road shield.
 * @property shieldUrl String
 * @property shieldIcon ByteArray
 * @property mapboxShield MapboxShield
 */
data class RoadShield @JvmOverloads constructor(
    val shieldUrl: String,
    var shieldIcon: ByteArray,
    val mapboxShield: MapboxShield? = null
) {

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RoadShield

        if (shieldUrl != other.shieldUrl) return false
        if (!shieldIcon.contentEquals(other.shieldIcon)) return false
        if (mapboxShield != other.mapboxShield) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = shieldUrl.hashCode()
        result = 31 * result + shieldIcon.contentHashCode()
        result = 31 * result + (mapboxShield?.hashCode() ?: 0)
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "RoadShield(" +
            "shieldUrl='$shieldUrl', " +
            "shieldIcon=${shieldIcon.contentToString()}, " +
            "mapboxShield=$mapboxShield" +
            ")"
    }
}
