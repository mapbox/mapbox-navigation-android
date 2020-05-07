package com.mapbox.navigation.core.replay.route2

/**
 * Replay route simulates a driver on a direction route. These options
 * allow you to control the behavior of that driver.
 *
 * Note that the default values are recommended because they have been tested.
 */
class ReplayRouteOptions(
    /**
     * Max speed the driver will drive on straight-aways
     */
    val maxSpeedMps: Double,
    /**
     * Speed the driver will slow down for turns approaching 90 degrees
     */
    val turnSpeedMps: Double,
    /**
     * Speed the driver will go when facing a u-turn
     */
    val uTurnSpeedMps: Double,
    /**
     * How fast the driver will accelerate to [maxSpeedMps] in mps^2
     */
    val maxAcceleration: Double,
    /**
     * How fast the driver will decelerate in mps^2
     */
    val minAcceleration: Double
) {
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
         */
        fun maxSpeedMps(maxSpeedMps: Double): Builder {
            this.maxSpeedMps = maxSpeedMps
            return this
        }

        /**
         * Speed the driver will slow down for turns approaching 90 degrees
         */
        fun turnSpeedMps(minSpeedMps: Double): Builder {
            this.turnSpeedMps = minSpeedMps
            return this
        }

        /**
         * Speed the driver will go when facing a u-turn
         */
        fun uTurnSpeedMps(uTurnSpeedMps: Double): Builder {
            this.uTurnSpeedMps = uTurnSpeedMps
            return this
        }

        /**
         * How fast the driver will accelerate to [maxSpeedMps] in mps^2
         */
        fun maxAcceleration(maxAcceleration: Double): Builder {
            this.maxAcceleration = maxAcceleration
            return this
        }

        /**
         * How fast the driver will decelerate in mps^2
         */
        fun minAcceleration(minAcceleration: Double): Builder {
            this.minAcceleration = minAcceleration
            return this
        }
    }
}
