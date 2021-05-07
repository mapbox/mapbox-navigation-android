package com.mapbox.navigation.base.route

import com.mapbox.api.directions.v5.models.RouteOptions
import java.util.concurrent.TimeUnit

/**
 * The options available for observing alternative routes. The [RouteOptions] used to
 * set the current active route are updated according to the current route progress.
 *
 * Register your [RouteAlternativesObserver] through [MapboxNavigation], the onRouteAlternatives
 * callback will be triggered every [intervalMillis]. There are multiple conditions that will
 * disable alternative route requests:
 *   - There are no observers registered
 *   - There is not an active route
 *   - The trip session is not started
 *
 * @param intervalMillis The interval in milliseconds, default is 5 min.
 */
class RouteAlternativesOptions private constructor(
    val intervalMillis: Long
) {
    /**
     * @return the builder that created the [RouteAlternativesOptions]
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

        other as RouteAlternativesOptions

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
     * Build your [RouteAlternativesOptions].
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
        fun build(): RouteAlternativesOptions {
            check(intervalMillis >= MINIMUM_REQUEST_ALTERNATIVES_INTERVAL) {
                "Route alternatives interval out of range" +
                    " $intervalMillis < $MINIMUM_REQUEST_ALTERNATIVES_INTERVAL"
            }
            return RouteAlternativesOptions(
                intervalMillis = intervalMillis
            )
        }
    }

    private companion object {
        private val MINIMUM_REQUEST_ALTERNATIVES_INTERVAL = TimeUnit.SECONDS.toMillis(10)
    }
}
