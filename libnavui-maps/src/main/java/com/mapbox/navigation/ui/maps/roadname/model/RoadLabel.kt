package com.mapbox.navigation.ui.maps.roadname.model

/**
 * The state is returned when buildings have been queried.
 * @property roadName The name of the road
 * @property shield The shield associated with the roadName if available, otherwise null
 * @property shieldName The name of the shield
 */
class RoadLabel @JvmOverloads internal constructor(
    val roadName: String? = null,
    val shield: ByteArray? = null,
    val shieldName: String? = null
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RoadLabel

        if (roadName != other.roadName) return false
        if (shield != null) {
            if (other.shield == null) return false
            if (!shield.contentEquals(other.shield)) return false
        } else if (other.shield != null) return false
        if (shieldName != other.shieldName) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = roadName?.hashCode() ?: 0
        result = 31 * result + (shield?.contentHashCode() ?: 0)
        result = 31 * result + (shieldName?.hashCode() ?: 0)
        return result
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun toString(): String {
        return "RoadLabel(" +
            "roadName=$roadName, " +
            "shield=${shield?.contentToString()}, " +
            "shieldName=$shieldName" +
            ")"
    }
}
