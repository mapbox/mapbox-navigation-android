package com.mapbox.navigation.ui.maps.route.callout.model

import com.mapbox.maps.ViewAnnotationOptions
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Options for configuration of [MapboxRouteCalloutApi].
 *
 * @param routeCalloutType defines the possible callout type on the route lines
 * @param similarDurationDelta defines the delta between primary and alternative durations
 * to consider their ETA similar
 * @param maxZoom max zoom level at which the view callout will be visible
 * @param minZoom min zoom level at which the view callout will be visible
 * @param priority callouts with higher priority will be shown on top of
 * [DVAs](https://docs.mapbox.com/android/maps/guides/annotations/view-annotations/#dynamic-view-annotations)
 * with lower priority
 */
@ExperimentalPreviewMapboxNavigationAPI
class MapboxRouteCalloutApiOptions private constructor(
    val routeCalloutType: RouteCalloutType,
    val similarDurationDelta: Duration,
    val maxZoom: Float?,
    val minZoom: Float?,
    val priority: Long?,
) {

    /**
     * Get a builder to customize a subset of current options.
     */
    fun toBuilder(): Builder = Builder().apply {
        routeCalloutType(routeCalloutType)
        similarDurationDelta(similarDurationDelta)
        maxZoom(maxZoom)
        minZoom(minZoom)
        priority(priority)
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
        if (maxZoom != other.maxZoom) return false
        if (minZoom != other.minZoom) return false
        if (priority != other.priority) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = routeCalloutType.hashCode()
        result = 31 * result + similarDurationDelta.hashCode()
        result = 31 * result + maxZoom.hashCode()
        result = 31 * result + minZoom.hashCode()
        result = 31 * result + priority.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "MapboxRouteCalloutApiOptions(" +
            "routeCalloutType=$routeCalloutType," +
            "similarDurationDelta=$similarDurationDelta, " +
            "maxZoom=$maxZoom, " +
            "minZoom=$minZoom, " +
            "priority=$priority" +
            ")"
    }

    class Builder {
        private var routeCalloutType = RouteCalloutType.RouteDurations
        private var similarDurationDelta = 3.minutes
        private var maxZoom: Float? = null
        private var minZoom: Float? = null
        private var priority: Long? = null

        /**
         * Defines the possible callout type on the route lines
         */
        fun routeCalloutType(type: RouteCalloutType): Builder = apply { routeCalloutType = type }

        /**
         * Defines the delta between primary and alternative durations to consider their ETA similar
         */
        fun similarDurationDelta(value: Duration): Builder = apply { similarDurationDelta = value }

        /**
         * Min zoom level at which the view callout will be visible.
         *
         * @see [ViewAnnotationOptions.getMinZoom]
         */
        fun minZoom(value: Float?) = apply {
            minZoom = value
        }

        /**
         * Max zoom level at which the view callout will be visible.
         *
         * @see [ViewAnnotationOptions.maxZoom]
         */
        fun maxZoom(value: Float?) = apply {
            maxZoom = value
        }

        /**
         * Callouts with higher priority will be shown on top of
         * [DVAs](https://docs.mapbox.com/android/maps/guides/annotations/view-annotations/#dynamic-view-annotations)
         * with lower priority.
         *
         * @see [ViewAnnotationOptions.getPriority]
         */
        fun priority(value: Long?) = apply {
            priority = value
        }

        /**
         * Build the [MapboxRouteCalloutApiOptions]
         */
        fun build(): MapboxRouteCalloutApiOptions {
            return MapboxRouteCalloutApiOptions(
                routeCalloutType,
                similarDurationDelta,
                maxZoom,
                minZoom,
                priority,
            )
        }
    }
}
