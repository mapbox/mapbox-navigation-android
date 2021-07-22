package com.mapbox.navigation.base.route

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.LegAnnotation
import com.mapbox.api.directions.v5.models.RouteOptions
import java.util.concurrent.TimeUnit

/**
 * The options available for refreshing the active [DirectionsRoute]. Each refresh will update
 * the current route's [LegAnnotation]. This includes traffic congestion and estimated travel time.
 *
 * Make sure that [RouteOptions.enableRefresh] is true to take advantage of this feature.
 *
 * @param intervalMillis The refresh interval in milliseconds, default is 5 min.
 */
class RouteRefreshOptions private constructor(
    val intervalMillis: Long
) {
    /**
     * @return the builder that created the [RouteRefreshOptions]
     */
    fun toBuilder(): Builder = Builder().apply {
        intervalMillis(intervalMillis)
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RouteRefreshOptions

        if (intervalMillis != other.intervalMillis) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        return intervalMillis.hashCode()
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "RouteRefreshOptions(" +
            "intervalMillis=$intervalMillis" +
            ")"
    }

    /**
     * Build your [RouteRefreshOptions].
     */
    class Builder {
        private var intervalMillis: Long = TimeUnit.MINUTES.toMillis(5)

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
                intervalMillis = intervalMillis
            )
        }
    }

    private companion object {
        private val MINIMUM_REFRESH_INTERVAL = TimeUnit.SECONDS.toMillis(30)
    }
}
