package com.mapbox.navigation.ui.maps.route.callout.model

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Options for configuration of [MapboxRouteCalloutApi].
 *
 * @param routeCalloutType defines the possible callout type on the route lines
 * @param similarDurationDelta defines the delta between primary and alternative durations
 * to consider their ETA similar
 */
@ExperimentalPreviewMapboxNavigationAPI
class MapboxRouteCalloutApiOptions private constructor(
    val routeCalloutType: RouteCalloutType,
    val similarDurationDelta: Duration,
) {

    /**
     * Get a builder to customize a subset of current options.
     */
    fun toBuilder(): Builder = Builder().apply {
        routeCalloutType(routeCalloutType)
        similarDurationDelta(similarDurationDelta)
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MapboxRouteCalloutApiOptions

        if (routeCalloutType != other.routeCalloutType) return false
        if (similarDurationDelta != other.similarDurationDelta) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = routeCalloutType.hashCode()
        result = 31 * result + similarDurationDelta.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "MapboxRouteCalloutApiOptions(" +
            "routeCalloutType=$routeCalloutType," +
            "similarDurationDelta=$similarDurationDelta" +
            ")"
    }

    class Builder {
        private var routeCalloutType = RouteCalloutType.RouteDurations
        private var similarDurationDelta = 3.minutes

        /**
         * Defines the possible callout type on the route lines
         */
        fun routeCalloutType(type: RouteCalloutType): Builder = apply { routeCalloutType = type }

        /**
         * Defines the delta between primary and alternative durations to consider their ETA similar
         */
        fun similarDurationDelta(value: Duration): Builder = apply { similarDurationDelta = value }

        /**
         * Build the [MapboxRouteCalloutApiOptions]
         */
        fun build(): MapboxRouteCalloutApiOptions {
            return MapboxRouteCalloutApiOptions(routeCalloutType, similarDurationDelta)
        }
    }
}
