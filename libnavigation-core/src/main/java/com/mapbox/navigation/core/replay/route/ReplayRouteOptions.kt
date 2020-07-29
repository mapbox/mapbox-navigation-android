package com.mapbox.navigation.core.replay.route

/**
 * Replay route simulates a driver on a direction route. These options
 * allow you to control the behavior of that driver.
 *
 * Note that the default values are recommended because they have been tested.
 *
 * @param maxSpeedMps Max speed the driver will drive on straight-aways
 * @param turnSpeedMps Speed the driver will slow down for turns approaching 90 degrees
 * @param uTurnSpeedMps Speed the driver will go when facing a u-turn
 * @param maxAcceleration How fast the driver will accelerate to [maxSpeedMps] in mps^2
 * @param minAcceleration How fast the driver will decelerate in mps^2
 */
class ReplayRouteOptions private constructor(
    val maxSpeedMps: Double,
    val turnSpeedMps: Double,
    val uTurnSpeedMps: Double,
    val maxAcceleration: Double,
    val minAcceleration: Double
) {
    /**
     * @return the builder that created the [ReplayRouteOptions]
     */
    fun toBuilder() = Builder().apply {
        maxSpeedMps(maxSpeedMps)
        turnSpeedMps(turnSpeedMps)
        uTurnSpeedMps(uTurnSpeedMps)
        maxAcceleration(maxAcceleration)
        minAcceleration(minAcceleration)
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ReplayRouteOptions

        if (maxSpeedMps != other.maxSpeedMps) return false
        if (turnSpeedMps != other.turnSpeedMps) return false
        if (uTurnSpeedMps != other.uTurnSpeedMps) return false
        if (maxAcceleration != other.maxAcceleration) return false
        if (minAcceleration != other.minAcceleration) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = maxSpeedMps.hashCode()
        result = 31 * result + turnSpeedMps.hashCode()
        result = 31 * result + uTurnSpeedMps.hashCode()
        result = 31 * result + maxAcceleration.hashCode()
        result = 31 * result + minAcceleration.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "ReplayRouteOptions(maxSpeedMps=$maxSpeedMps, turnSpeedMps=$turnSpeedMps, uTurnSpeedMps=$uTurnSpeedMps, maxAcceleration=$maxAcceleration, minAcceleration=$minAcceleration)"
    }

    /**
     * Used to build [ReplayRouteOptions].
     */
    class Builder {
        private var maxSpeedMps = 30.0
        private var turnSpeedMps = 3.0
        private var uTurnSpeedMps = 1.0
        private var maxAcceleration = 4.0
        private var minAcceleration = -4.0

        /**
         * Build your [ReplayRouteOptions].
         *
         * @return [ReplayRouteOptions]
         */
        fun build(): ReplayRouteOptions {
            return ReplayRouteOptions(
                maxSpeedMps = maxSpeedMps,
                turnSpeedMps = turnSpeedMps,
                uTurnSpeedMps = uTurnSpeedMps,
                maxAcceleration = maxAcceleration,
                minAcceleration = minAcceleration
            )
        }

        /**
         * Max speed the driver will drive on straight-aways
         *
         * @param maxSpeedMps
         * @return [Builder]
         */
        fun maxSpeedMps(maxSpeedMps: Double): Builder {
            this.maxSpeedMps = maxSpeedMps
            return this
        }

        /**
         * Speed the driver will slow down for turns approaching 90 degrees
         *
         * @param minSpeedMps
         * @return [Builder]
         */
        fun turnSpeedMps(minSpeedMps: Double): Builder {
            this.turnSpeedMps = minSpeedMps
            return this
        }

        /**
         * Speed the driver will go when facing a u-turn
         *
         * @param uTurnSpeedMps
         * @return [Builder]
         */
        fun uTurnSpeedMps(uTurnSpeedMps: Double): Builder {
            this.uTurnSpeedMps = uTurnSpeedMps
            return this
        }

        /**
         * How fast the driver will accelerate to [maxSpeedMps]
         *
         * @param maxAcceleration in mps^2
         * @return [Builder]
         */
        fun maxAcceleration(maxAcceleration: Double): Builder {
            this.maxAcceleration = maxAcceleration
            return this
        }

        /**
         * How fast the driver will decelerate
         *
         * @param minAcceleration in mps^2
         * @return [Builder]
         */
        fun minAcceleration(minAcceleration: Double): Builder {
            this.minAcceleration = minAcceleration
            return this
        }
    }
}
