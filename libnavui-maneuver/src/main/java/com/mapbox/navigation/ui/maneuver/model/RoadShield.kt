package com.mapbox.navigation.ui.maneuver.model

/**
 * Data structure  that holds information about road shield.
 * @property shieldUrl String
 * @property shieldIcon ByteArray
 */
data class RoadShield(
    val shieldUrl: String,
    var shieldIcon: ByteArray
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

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = shieldUrl.hashCode()
        result = 31 * result + shieldIcon.contentHashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "RoadShield(shieldUrl=$shieldUrl, shieldIcon=${shieldIcon.contentToString()})"
    }
}
