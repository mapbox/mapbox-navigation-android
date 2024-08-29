package com.mapbox.navigation.tripdata.shield.model

/**
 * Data structure that wraps the information contained in [RouteShield] and [RouteShieldOrigin]
 * and is used to render the shield.
 *
 * @property shield [RouteShield]
 * @property origin [RouteShieldOrigin]
 */
class RouteShieldResult internal constructor(
    val shield: RouteShield,
    val origin: RouteShieldOrigin,
) {
    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RouteShieldResult) return false

        if (shield != other.shield) return false
        if (origin != other.origin) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = shield.hashCode()
        result = 31 * result + origin.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "RouteShieldResult(shield=$shield, origin=$origin)"
    }
}
