package com.mapbox.navigation.base.route

import com.mapbox.api.directions.v5.WalkingOptions

/**
 * Class for specifying options for use with the walking profile.
 */
class NavigationWalkingOptions internal constructor(val walkingOptions: WalkingOptions) {

    companion object {
        /**
         * Build a new [WalkingOptions] object with no defaults.
         *
         * @return a [Builder] object for creating a [NavigationWalkingOptions] object
         */
        @JvmStatic
        fun builder(): Builder {
            return Builder(WalkingOptions.builder())
        }
    }

    /**
     * This builder is used to create a new object with specifications relating to walking directions.
     */
    class Builder internal constructor(private val builder: WalkingOptions.Builder) {

        /**
         * Builds a [NavigationWalkingOptions] object with the specified configurations.
         *
         * @return a NavigationWalkingOptions object
         */
        fun build(): NavigationWalkingOptions = NavigationWalkingOptions(builder.build())

        /**
         * Walking speed in meters per second. Must be between 0.14 and 6.94 meters per second.
         * Defaults to 1.42 meters per second
         *
         * @param walkingSpeed in meters per second
         * @return this builder
         */
        fun walkingSpeed(walkingSpeed: Double?): Builder {
            builder.walkingSpeed(walkingSpeed)
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
            builder.walkwayBias(walkwayBias)
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
            builder.alleyBias(alleyBias)
            return this
        }
    }
}
