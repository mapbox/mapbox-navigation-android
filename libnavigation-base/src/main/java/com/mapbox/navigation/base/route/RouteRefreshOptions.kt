package com.mapbox.navigation.base.route

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.LegAnnotation
import java.util.concurrent.TimeUnit

/**
 * The options available for refreshing the active [DirectionsRoute]. Each refresh will update
 * the current route's [LegAnnotation]. This includes traffic congestion and estimated travel time.
 *
 * @param enabled Periodically refreshes the route when enabled, defaults to true
 * @param intervalMillis The refresh interval in milliseconds, default is 5 min.
 */
class RouteRefreshOptions private constructor(
    val enabled: Boolean,
    val intervalMillis: Long
) {
    /**
     * @return the builder that created the [ArrivalOptions]
     */
    fun toBuilder(): Builder = Builder().apply {
        enabled(enabled)
        intervalMillis(intervalMillis)
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RouteRefreshOptions

        if (enabled != other.enabled) return false
        if (intervalMillis != other.intervalMillis) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = enabled.hashCode()
        result = 31 * result + (intervalMillis.hashCode())
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "RouteRefreshOptions(" +
            "enabled=$enabled, intervalMillis=$intervalMillis" +
            ")"
    }

    /**
     * Build your [RouteRefreshOptions].
     */
    class Builder {

        private var enabled: Boolean = true
        private var intervalMillis: Long = TimeUnit.MINUTES.toMillis(5)

        /**
         * Periodically refreshes the route when enabled, defaults to false
         *
         * See [com.mapbox.navigation.base.extensions.supportsRouteRefresh]
         * for a list of requirements that your route request needs to meet to be eligible for
         * refresh calls.
         */
        fun enabled(enabled: Boolean): Builder {
            this.enabled = enabled
            return this
        }

        /**
         * Update the route refresh interval in milliseconds.
         */
        fun intervalMillis(intervalMillis: Long): Builder {
            this.intervalMillis = intervalMillis
            return this
        }

        /**
         * Build the object.
         */
        fun build(): RouteRefreshOptions {
            check(intervalMillis >= MINIMUM_REFRESH_INTERVAL) {
                "Route refresh interval out of range $intervalMillis < $MINIMUM_REFRESH_INTERVAL"
            }
            return RouteRefreshOptions(
                enabled = enabled,
                intervalMillis = intervalMillis
            )
        }
    }

    private companion object {
        private val MINIMUM_REFRESH_INTERVAL = TimeUnit.SECONDS.toMillis(30)
    }
}
