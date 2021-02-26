package com.mapbox.navigation.ui.maps.camera.data

/**
 * Set of options used to customize [MapboxNavigationViewportDataSource].
 *
 * @param maxFollowingPitch the max pitch that will be generated for camera frames when following
 * @param minFollowingZoom the min zoom that will be generated for all following camera frames
 * @param maxZoom the max zoom that will be generated for all camera frames
 */
class MapboxNavigationViewportDataSourceOptions private constructor(
    val maxFollowingPitch: Double,
    val minFollowingZoom: Double,
    val maxZoom: Double
) {

    /**
     * Get a builder to customize a subset of current options.
     */
    fun toBuilder(): Builder = Builder().apply {
        maxFollowingPitch(maxFollowingPitch)
        minFollowingZoom(minFollowingZoom)
        maxZoom(maxZoom)
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MapboxNavigationViewportDataSourceOptions

        if (maxFollowingPitch != other.maxFollowingPitch) return false
        if (minFollowingZoom != other.minFollowingZoom) return false
        if (maxZoom != other.maxZoom) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = maxFollowingPitch.hashCode()
        result = 31 * result + minFollowingZoom.hashCode()
        result = 31 * result + maxZoom.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "MapboxNavigationViewportDataSourceOptions(" +
            "maxFollowingPitch=$maxFollowingPitch, " +
            "minFollowingZoom=$minFollowingZoom" +
            "maxZoom=$maxZoom" +
            ")"
    }

    /**
     * Build a new [MapboxNavigationViewportDataSourceOptions]
     */
    class Builder {
        private var maxFollowingPitch = 45.0
        private var minFollowingZoom = 12.0
        private var maxZoom = 16.35

        /**
         * Override [MapboxNavigationViewportDataSourceOptions.maxFollowingPitch].
         *
         * Defaults to 40.0.
         */
        fun maxFollowingPitch(maxFollowingPitch: Double): Builder = apply {
            this.maxFollowingPitch = maxFollowingPitch
        }

        /**
         * Override [MapboxNavigationViewportDataSourceOptions.minFollowingZoom].
         *
         * Defaults to 12.0.
         */
        fun minFollowingZoom(minFollowingZoom: Double): Builder = apply {
            this.minFollowingZoom = minFollowingZoom
        }

        /**
         * Override [MapboxNavigationViewportDataSourceOptions.maxZoom].
         *
         * Defaults to 19.0.
         */
        fun maxZoom(maxZoom: Double): Builder = apply {
            this.maxZoom = maxZoom
        }

        /**
         * Build a new instance of [MapboxNavigationViewportDataSourceOptions].
         */
        fun build(): MapboxNavigationViewportDataSourceOptions =
            MapboxNavigationViewportDataSourceOptions(
                maxFollowingPitch = maxFollowingPitch,
                minFollowingZoom = minFollowingZoom,
                maxZoom = maxZoom
            )
    }
}
