package com.mapbox.navigation.base.options

/**
 * Predictive cache Maps related options.
 *
 * @param predictiveCacheLocationOptions [PredictiveCacheLocationOptions] location configuration for visual map predictive caching
 * @param minZoom Minimum zoom level for the tile package. See **com.mapbox.maps.TilesetDescriptorOptions#getMinZoom**
 * @param maxZoom Maximum zoom level for the tile package. See **com.mapbox.maps.TilesetDescriptorOptions#getMaxZoom**
 */
class PredictiveCacheMapsOptions private constructor(
    val predictiveCacheLocationOptions: PredictiveCacheLocationOptions,
    val minZoom: Byte,
    val maxZoom: Byte,
) {

    /**
     * Get a builder to customize a subset of current options.
     */
    fun toBuilder(): Builder = Builder().apply {
        predictiveCacheLocationOptions(predictiveCacheLocationOptions)
        minZoom(minZoom)
        maxZoom(maxZoom)
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PredictiveCacheMapsOptions

        if (predictiveCacheLocationOptions != other.predictiveCacheLocationOptions) return false
        if (minZoom != other.minZoom) return false
        if (maxZoom != other.maxZoom) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = predictiveCacheLocationOptions.hashCode()
        result = 31 * result + minZoom.hashCode()
        result = 31 * result + maxZoom.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "PredictiveCacheMapsOptions(" +
            "predictiveCacheLocationOptions=$predictiveCacheLocationOptions, " +
            "minZoom=$minZoom, " +
            "maxZoom=$maxZoom" +
            ")"
    }

    /**
     * Build a new [PredictiveCacheMapsOptions].
     */
    class Builder {
        private var predictiveCacheLocationOptions: PredictiveCacheLocationOptions? = null
        private var minZoom = 15.toByte()
        private var maxZoom = 16.toByte()

        /**
         * Location configuration for visual map predictive caching
         */
        fun predictiveCacheLocationOptions(
            predictiveCacheLocationOptions: PredictiveCacheLocationOptions
        ): Builder = also { this.predictiveCacheLocationOptions = predictiveCacheLocationOptions }

        /**
         * Minimum zoom level for the tile package.
         * See **com.mapbox.maps.TilesetDescriptorOptions#getMinZoom**
         *
         * Defaults to 0
         */
        fun minZoom(minZoom: Byte): Builder = apply {
            this.minZoom = minZoom
        }

        /**
         * Maximum zoom level for the tile package.
         * See **com.mapbox.maps.TilesetDescriptorOptions#getMaxZoom**
         *
         * Defaults to 16
         */
        fun maxZoom(maxZoom: Byte): Builder = apply {
            this.maxZoom = maxZoom
        }

        /**
         * Build [PredictiveCacheMapsOptions].
         */
        fun build(): PredictiveCacheMapsOptions = PredictiveCacheMapsOptions(
            predictiveCacheLocationOptions ?: PredictiveCacheLocationOptions.Builder().build(),
            minZoom,
            maxZoom,
        )
    }
}
