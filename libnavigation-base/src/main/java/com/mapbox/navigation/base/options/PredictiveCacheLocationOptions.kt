package com.mapbox.navigation.base.options

/**
 * PredictiveCacheLocationOptions.
 *
 * @param currentLocationRadiusInMeters How far around the user's location we're going to cache, in meters. Defaults to 20000 (20 km)
 * @param routeBufferRadiusInMeters How far around the active route we're going to cache, in meters (if route is set). Defaults to 5000 (5 km)
 * @param destinationLocationRadiusInMeters How far around the destination location we're going to cache, in meters (if route is set). Defaults to 50000 (50 km)
 */
class PredictiveCacheLocationOptions private constructor(
    val currentLocationRadiusInMeters: Int,
    val routeBufferRadiusInMeters: Int,
    val destinationLocationRadiusInMeters: Int,
) {

    /**
     * Get a builder to customize a subset of current options.
     */
    fun toBuilder(): Builder = Builder().apply {
        currentLocationRadiusInMeters(currentLocationRadiusInMeters)
        routeBufferRadiusInMeters(routeBufferRadiusInMeters)
        destinationLocationRadiusInMeters(destinationLocationRadiusInMeters)
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

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = currentLocationRadiusInMeters.hashCode()
        result = 31 * result + routeBufferRadiusInMeters.hashCode()
        result = 31 * result + destinationLocationRadiusInMeters.hashCode()
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
            ")"
    }

    /**
     * Build a new [PredictiveCacheLocationOptions]
     */
    class Builder {

        private var currentLocationRadiusInMeters: Int = 20_000
        private var routeBufferRadiusInMeters: Int = 5_000
        private var destinationLocationRadiusInMeters: Int = 50_000

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
         * Build the [PredictiveCacheLocationOptions]
         */
        fun build(): PredictiveCacheLocationOptions {
            return PredictiveCacheLocationOptions(
                currentLocationRadiusInMeters = currentLocationRadiusInMeters,
                routeBufferRadiusInMeters = routeBufferRadiusInMeters,
                destinationLocationRadiusInMeters = destinationLocationRadiusInMeters,
            )
        }
    }
}
