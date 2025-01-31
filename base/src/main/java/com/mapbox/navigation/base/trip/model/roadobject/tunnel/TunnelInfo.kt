package com.mapbox.navigation.base.trip.model.roadobject.tunnel

/**
 * Tunnel information.
 */
class TunnelInfo internal constructor(
    /**
     * Tunnel name.
     */
    val name: String?,
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TunnelInfo

        if (name != other.name) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        return name.hashCode()
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "TunnelInfo(name='$name')"
    }
}
