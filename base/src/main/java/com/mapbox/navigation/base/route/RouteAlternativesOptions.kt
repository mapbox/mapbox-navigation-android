package com.mapbox.navigation.base.route

import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.internal.route.DEFAULT_AVOID_MANEUVER_SECONDS_FOR_ROUTE_ALTERNATIVES
import com.mapbox.navigator.RouteAlternativesObserver
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
 * @param intervalMillis The interval in milliseconds, default is 5 min. Minimum is 30 seconds.
 * @param avoidManeuverSeconds a radius in seconds around origin point where need to
 * avoid any significant maneuvers. Unit is seconds, default is 8 second.
 */
class RouteAlternativesOptions private constructor(
    val intervalMillis: Long,
    val avoidManeuverSeconds: Int,
) {
    /**
     * @return the builder that created the [RouteAlternativesOptions]
     */
    fun toBuilder(): Builder = Builder().apply {
        intervalMillis(intervalMillis)
        avoidManeuverSeconds(avoidManeuverSeconds)
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RouteAlternativesOptions

        if (intervalMillis != other.intervalMillis) return false
        if (avoidManeuverSeconds != other.avoidManeuverSeconds) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = intervalMillis.hashCode()
        result = 31 * result + avoidManeuverSeconds
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "RouteRefreshOptions(" +
            "intervalMillis=$intervalMillis, " +
            "avoidManeuverSeconds=$avoidManeuverSeconds" +
            ")"
    }

    /**
     * Build your [RouteAlternativesOptions].
     */
    class Builder {
        private var intervalMillis: Long = TimeUnit.MINUTES.toMillis(5)
        private var avoidManeuverSeconds = DEFAULT_AVOID_MANEUVER_SECONDS_FOR_ROUTE_ALTERNATIVES

        /**
         * Update the route refresh interval in milliseconds.
         * Default is 5 minutes, minimum is 30 seconds.
         */
        fun intervalMillis(intervalMillis: Long): Builder = apply {
            check(intervalMillis >= MINIMUM_REQUEST_ALTERNATIVES_INTERVAL) {
                "Route alternatives interval out of range" +
                    " $intervalMillis < $MINIMUM_REQUEST_ALTERNATIVES_INTERVAL"
            }
            this.intervalMillis = intervalMillis
        }

        /**
         * Avoid maneuver second. A radius in seconds around origin point where need to
         * avoid any significant maneuvers. Unit is seconds.
         *
         * Default value is **8**.
         *
         * @throws IllegalStateException if provided value is less than **0**
         */
        fun avoidManeuverSeconds(avoidManeuverSeconds: Int): Builder = apply {
            check(avoidManeuverSeconds >= MINIMUM_AVOID_MANEUVER_SECONDS) {
                "avoidManeuverSeconds out of range" +
                    " $avoidManeuverSeconds < $MINIMUM_AVOID_MANEUVER_SECONDS"
            }
            this.avoidManeuverSeconds = avoidManeuverSeconds
        }

        /**
         * Build the object.
         */
        fun build() = RouteAlternativesOptions(intervalMillis, avoidManeuverSeconds)

        private companion object {
            private val MINIMUM_REQUEST_ALTERNATIVES_INTERVAL = TimeUnit.SECONDS.toMillis(30)
            private const val MINIMUM_AVOID_MANEUVER_SECONDS = 0
        }
    }
}
