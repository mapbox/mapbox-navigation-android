package com.mapbox.navigation.base.options

import com.mapbox.api.directions.v5.models.RouteOptions

/**
 * Reroute options
 *
 * @param avoidManeuverSeconds a radius in seconds around reroute origin point where need to
 * avoid any significant maneuvers. Unit is seconds.
 */
class RerouteOptions private constructor(
    val avoidManeuverSeconds: Int
) {

    /**
     * @return the builder that created the [RerouteOptions]
     */
    fun toBuilder(): Builder = Builder().apply {
        avoidManeuverSeconds(avoidManeuverSeconds)
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RerouteOptions

        if (avoidManeuverSeconds != other.avoidManeuverSeconds) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        return avoidManeuverSeconds.hashCode()
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String = "RerouteOptions(" +
        "avoidManeuverSeconds=$avoidManeuverSeconds" +
        ")"

    /**
     * Builder for [RerouteOptions].
     */
    class Builder {
        private var avoidManeuverSeconds = 8

        /**
         * Avoid maneuver second. A radius in seconds around reroute origin point where need to
         * avoid any significant maneuvers. Unit is seconds.
         *
         * Default value is **8**.
         *
         * Note: value is mapped to _meters_ at a re-route moment (based on speed), if meters result
         * is more than 1000, it's restricted to 1000 meters.
         *
         * @throws IllegalStateException if provided value is less than **0**
         * @see RouteOptions.avoidManeuverRadius
         */
        fun avoidManeuverSeconds(avoidManeuverSeconds: Int): Builder = apply {
            check(avoidManeuverSeconds >= 0) {
                "avoidManeuverSeconds must be >= 0"
            }
            this.avoidManeuverSeconds = avoidManeuverSeconds
        }

        /**
         * Build the [RerouteOptions]
         */
        fun build(): RerouteOptions = RerouteOptions(avoidManeuverSeconds)
    }
}
