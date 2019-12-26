package com.mapbox.services.android.navigation.v5.navigation

import com.google.gson.annotations.SerializedName

/**
 * Class for specifying options for use with the walking profile.
 *
 * @param walkingSpeed Walking speed in meters per second. Must be between 0.14 and 6.94 meters per second.
 * Defaults to 1.42 meters per second
 *
 * @param walkwayBias A bias which determines whether the route should prefer or avoid the use of roads or paths
 * that are set aside for pedestrian-only use (walkways). The allowed range of values is from
 * -1 to 1, where -1 indicates indicates preference to avoid walkways, 1 indicates preference
 * to favor walkways, and 0 indicates no preference (the default).
 *
 * @param alleyBias A bias which determines whether the route should prefer or avoid the use of alleys. The
 * allowed range of values is from -1 to 1, where -1 indicates indicates preference to avoid
 * alleys, 1 indicates preference to favor alleys, and 0 indicates no preference (the default).
 */
data class WalkingOptionsNavigation(
    @SerializedName("walking_speed") val walkingSpeed: Double? = null,
    @SerializedName("walkway_bias") val walkwayBias: Double? = null,
    @SerializedName("alley_bias") val alleyBias: Double? = null
) {
    companion object {
        /**
         * Build a new [WalkingOptionsNavigation] object with no defaults.
         *
         * @return a [Builder] object for creating a [WalkingOptionsNavigation] object
         */
        @JvmStatic
        fun builder(): Builder {
            return Builder()
        }
    }

    /**
     * This builder is used to create a new object with specifications relating to walking directions.
     */
    class Builder internal constructor() {
        var walkingSpeed: Double? = null
        var walkwayBias: Double? = null
        var alleyBias: Double? = null

        /**
         * Builds a [WalkingOptionsNavigation] object with the specified configurations.
         *
         * @return a WalkingOptionsNavigation object
         */
        fun build(): WalkingOptionsNavigation = WalkingOptionsNavigation(
            walkingSpeed = walkingSpeed,
            walkwayBias = walkwayBias,
            alleyBias = alleyBias
        )

        /**
         * Walking speed in meters per second. Must be between 0.14 and 6.94 meters per second.
         * Defaults to 1.42 meters per second
         *
         * @param walkingSpeed in meters per second
         * @return this builder
         */
        fun walkingSpeed(walkingSpeed: Double?): Builder {
            this.walkingSpeed = walkingSpeed
            return this
        }

        /**
         * A bias which determines whether the route should prefer or avoid the use of roads or paths
         * that are set aside for pedestrian-only use (walkways). The allowed range of values is from
         * -1 to 1, where -1 indicates preference to avoid walkways, 1 indicates preference to favor
         * walkways, and 0 indicates no preference (the default).
         *
         * @param walkwayBias bias to prefer or avoid walkways
         * @return this builder
         */
        fun walkwayBias(walkwayBias: Double?): Builder {
            this.walkwayBias = walkwayBias
            return this
        }

        /**
         * A bias which determines whether the route should prefer or avoid the use of alleys. The
         * allowed range of values is from -1 to 1, where -1 indicates preference to avoid alleys, 1
         * indicates preference to favor alleys, and 0 indicates no preference (the default).
         *
         * @param alleyBias bias to prefer or avoid alleys
         * @return this builder
         */
        fun alleyBias(alleyBias: Double?): Builder {
            this.alleyBias = alleyBias
            return this
        }
    }
}
