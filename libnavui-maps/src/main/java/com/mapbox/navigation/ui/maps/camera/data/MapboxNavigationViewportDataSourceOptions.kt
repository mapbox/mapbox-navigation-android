package com.mapbox.navigation.ui.maps.camera.data

/**
 * Set of options used to customize [MapboxNavigationViewportDataSource].
 *
 * @param maxFollowingPitch the max pitch that will be generate for camera frames when following
 * @param maxZoom the max zoom that will be generate for all camera frames
 */
class MapboxNavigationViewportDataSourceOptions private constructor(
    val maxFollowingPitch: Double,
    val maxZoom: Double
) {

    /**
     * Get a builder to customize a subset of current options.
     */
    fun toBuilder(): Builder = Builder().apply {
        maxFollowingPitch(maxFollowingPitch)
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
        if (maxZoom != other.maxZoom) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = maxFollowingPitch.hashCode()
        result = 31 * result + maxZoom.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "MapboxNavigationViewportDataSourceOptions(" +
            "maxFollowingPitch=$maxFollowingPitch, " +
            "maxZoom=$maxZoom" +
            ")"
    }

    /**
     * Build a new [MapboxNavigationViewportDataSourceOptions]
     */
    class Builder {
        private var maxFollowingPitch = 40.0
        private var maxZoom = 19.0

        /**
         * Override [MapboxNavigationViewportDataSourceOptions.maxFollowingPitch].
         *
         * Defaults to 40.0.
         */
        fun maxFollowingPitch(maxFollowingPitch: Double): Builder = apply {
            this.maxFollowingPitch = maxFollowingPitch
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
                maxFollowingPitch,
                maxZoom
            )
    }
}
