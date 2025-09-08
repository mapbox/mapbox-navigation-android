package com.mapbox.navigation.base.options

import androidx.annotation.RestrictTo
import com.mapbox.navigator.PredictiveLocationTrackerOptions

/**
 * PredictiveCacheLocationOptions.
 *
 * @param currentLocationRadiusInMeters How far around the user's location we're going to cache, in meters. Defaults to 20000 (20 km)
 * @param routeBufferRadiusInMeters How far around the active route we're going to cache, in meters (if route is set). Defaults to 5000 (5 km)
 * @param destinationLocationRadiusInMeters How far around the destination location we're going to cache, in meters (if route is set). Defaults to 50000 (50 km)
 * @param loadPredictiveCacheForAlternativeRoutes Whether alternative routes will be loaded for predictive cache. Defaults to false.
 */
class PredictiveCacheLocationOptions private constructor(
    val currentLocationRadiusInMeters: Int,
    val routeBufferRadiusInMeters: Int,
    val destinationLocationRadiusInMeters: Int,
    val loadPredictiveCacheForAlternativeRoutes: Boolean,
) {

    /**
     * Get a builder to customize a subset of current options.
     */
    fun toBuilder(): Builder = Builder().apply {
        currentLocationRadiusInMeters(currentLocationRadiusInMeters)
        routeBufferRadiusInMeters(routeBufferRadiusInMeters)
        destinationLocationRadiusInMeters(destinationLocationRadiusInMeters)
        loadPredictiveCacheForAlternativeRoutes(loadPredictiveCacheForAlternativeRoutes)
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PredictiveCacheLocationOptions

        if (currentLocationRadiusInMeters != other.currentLocationRadiusInMeters) return false
        if (routeBufferRadiusInMeters != other.routeBufferRadiusInMeters) return false
        if (destinationLocationRadiusInMeters != other.destinationLocationRadiusInMeters) {
            return false
        }
        if (loadPredictiveCacheForAlternativeRoutes !=
            other.loadPredictiveCacheForAlternativeRoutes
        ) {
            return false
        }

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = currentLocationRadiusInMeters.hashCode()
        result = 31 * result + routeBufferRadiusInMeters.hashCode()
        result = 31 * result + destinationLocationRadiusInMeters.hashCode()
        result = 31 * result + loadPredictiveCacheForAlternativeRoutes.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "PredictiveCacheLocationOptions(" +
            "currentLocationRadiusInMeters=$currentLocationRadiusInMeters, " +
            "routeBufferRadiusInMeters=$routeBufferRadiusInMeters, " +
            "destinationLocationRadiusInMeters=$destinationLocationRadiusInMeters" +
            "loadPredictiveCacheForAlternativeRoutes=$loadPredictiveCacheForAlternativeRoutes" +
            ")"
    }

    /**
     * Build a new [PredictiveCacheLocationOptions]
     */
    class Builder {

        private var currentLocationRadiusInMeters: Int = 20_000
        private var routeBufferRadiusInMeters: Int = 5_000
        private var destinationLocationRadiusInMeters: Int = 50_000
        private var loadPredictiveCacheForAlternativeRoutes: Boolean = false

        /**
         * How far around the user's location we're going to cache, in meters. Defaults to 20000 (20 km)
         */
        fun currentLocationRadiusInMeters(radiusInMeters: Int): Builder =
            apply { this.currentLocationRadiusInMeters = radiusInMeters }

        /**
         * How far around the active route we're going to cache, in meters (if route is set). Defaults to 5000 (5 km)
         */
        fun routeBufferRadiusInMeters(radiusInMeters: Int): Builder =
            apply { this.routeBufferRadiusInMeters = radiusInMeters }

        /**
         * How far around the destination location we're going to cache, in meters (if route is set). Defaults to 50000 (50 km)
         */
        fun destinationLocationRadiusInMeters(radiusInMeters: Int): Builder =
            apply { this.destinationLocationRadiusInMeters = radiusInMeters }

        /**
         * Whether alternative routes will be loaded for predictive cache. Defaults to false.
         */
        fun loadPredictiveCacheForAlternativeRoutes(load: Boolean): Builder =
            apply { this.loadPredictiveCacheForAlternativeRoutes = load }

        /**
         * Build the [PredictiveCacheLocationOptions]
         */
        fun build(): PredictiveCacheLocationOptions {
            return PredictiveCacheLocationOptions(
                currentLocationRadiusInMeters = currentLocationRadiusInMeters,
                routeBufferRadiusInMeters = routeBufferRadiusInMeters,
                destinationLocationRadiusInMeters = destinationLocationRadiusInMeters,
                loadPredictiveCacheForAlternativeRoutes = loadPredictiveCacheForAlternativeRoutes,
            )
        }
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun PredictiveCacheLocationOptions.toPredictiveLocationTrackerOptions() =
    PredictiveLocationTrackerOptions(
        currentLocationRadiusInMeters,
        routeBufferRadiusInMeters,
        destinationLocationRadiusInMeters,
        loadPredictiveCacheForAlternativeRoutes,
    )
