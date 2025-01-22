package com.mapbox.navigation.core.replay.route

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Options to use with the [ReplayRouteSession].
 *
 * @param replayRouteOptions [ReplayRouteOptions] to change the simulated driver
 * @param locationResetEnabled Reset to device location when route no route is set
 * @param decodeMinDistance Decode the geometry distance in kilometers at a time
 */
@ExperimentalPreviewMapboxNavigationAPI
class ReplayRouteSessionOptions private constructor(
    val replayRouteOptions: ReplayRouteOptions,
    val locationResetEnabled: Boolean,
    val decodeMinDistance: Double,
) {
    /**
     * @return the builder that created the [ReplayRouteSessionOptions]
     */
    fun toBuilder(): Builder = Builder().apply {
        replayRouteOptions(replayRouteOptions)
        locationResetEnabled(locationResetEnabled)
        decodeMinDistance(decodeMinDistance)
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ReplayRouteSessionOptions

        if (replayRouteOptions != other.replayRouteOptions) return false
        if (locationResetEnabled != other.locationResetEnabled) return false
        if (decodeMinDistance != other.decodeMinDistance) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = replayRouteOptions.hashCode()
        result = 31 * result + locationResetEnabled.hashCode()
        result = 31 * result + decodeMinDistance.hashCode()
        return result
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun toString(): String {
        return "ReplayRouteSessionOptions(" +
            "replayRouteOptions=$replayRouteOptions, " +
            "locationResetEnabled=$locationResetEnabled, " +
            "decodeMinDistance=$decodeMinDistance" +
            ")"
    }

    /**
     * Used to build [ReplayRouteSession].
     */
    @ExperimentalPreviewMapboxNavigationAPI
    class Builder {
        private var replayRouteOptions = ReplayRouteOptions.Builder().build()
        private var locationResetEnabled = true
        private var decodeMinDistance = 100.0 // 100 kilometers

        /**
         * Build your [ReplayRouteSessionOptions].
         *
         * @return [ReplayRouteSessionOptions]
         */
        fun build(): ReplayRouteSessionOptions = ReplayRouteSessionOptions(
            replayRouteOptions = replayRouteOptions,
            locationResetEnabled = locationResetEnabled,
            decodeMinDistance = decodeMinDistance,
        )

        /**
         * Set the [ReplayRouteOptions]. The next time a batch of locations are decoded the options
         * will be used. This will not effect the locations that are already simulated.
         *
         * @see ReplayRouteMapper
         * @param replayRouteOptions
         * @return [Builder]
         */
        fun replayRouteOptions(replayRouteOptions: ReplayRouteOptions): Builder = apply {
            this.replayRouteOptions = replayRouteOptions
        }

        /**
         * The replay session will use the device location when the route is cleared. If you do not
         * want this behavior disable with `false`.
         *
         * @param locationResetEnabled
         * @return [Builder]
         */
        fun locationResetEnabled(locationResetEnabled: Boolean): Builder = apply {
            this.locationResetEnabled = locationResetEnabled
        }

        /**
         * The [DirectionsRoute.geometry] is decoded when needed. This defines a minimum distance
         * in kilometers to decode at a time.
         *
         * @param decodeDistance
         * @return [Builder]
         */
        fun decodeMinDistance(decodeDistance: Double): Builder = apply {
            this.decodeMinDistance = decodeDistance
        }
    }
}
