package com.mapbox.navigation.base.options

import com.mapbox.bindgen.Value
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Predictive cache Maps related options.
 *
 * @param predictiveCacheLocationOptions [PredictiveCacheLocationOptions] location configuration for visual map predictive caching
 * @param minZoom Minimum zoom level for the tile package. See **com.mapbox.maps.TilesetDescriptorOptions#getMinZoom**
 * @param maxZoom Maximum zoom level for the tile package. See **com.mapbox.maps.TilesetDescriptorOptions#getMaxZoom**
 * @param extraOptions Extra tileset descriptor options. See **com.mapbox.maps.TilesetDescriptorOptions#getExtraOptions**
 * @param tilesets The tilesets associated with the tileset descriptor. See **com.mapbox.maps.TilesetDescriptorOptions#getTilesets**
 */
class PredictiveCacheMapsOptions private constructor(
    val predictiveCacheLocationOptions: PredictiveCacheLocationOptions,
    val minZoom: Byte,
    val maxZoom: Byte,
    val extraOptions: Value?,
    val tilesets: List<String>?,
) {

    /**
     * Get a builder to customize a subset of current options.
     */
    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    fun toBuilder(): Builder = Builder().apply {
        predictiveCacheLocationOptions(predictiveCacheLocationOptions)
        minZoom(minZoom)
        maxZoom(maxZoom)
        extraOptions(extraOptions)
        tilesets(tilesets)
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
        if (extraOptions != other.extraOptions) return false
        return tilesets == other.tilesets
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = predictiveCacheLocationOptions.hashCode()
        result = 31 * result + minZoom
        result = 31 * result + maxZoom
        result = 31 * result + (extraOptions?.hashCode() ?: 0)
        result = 31 * result + (tilesets?.hashCode() ?: 0)
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "PredictiveCacheMapsOptions(" +
            "predictiveCacheLocationOptions=$predictiveCacheLocationOptions, " +
            "minZoom=$minZoom, " +
            "maxZoom=$maxZoom, " +
            "extraOptions=$extraOptions, " +
            "tilesets=$tilesets" +
            ")"
    }

    /**
     * Build a new [PredictiveCacheMapsOptions].
     */
    class Builder {
        private var predictiveCacheLocationOptions: PredictiveCacheLocationOptions? = null
        private var minZoom = 15.toByte()
        private var maxZoom = 16.toByte()
        private var extraOptions: Value? = null
        private var tilesets: List<String>? = null

        /**
         * Location configuration for visual map predictive caching
         */
        fun predictiveCacheLocationOptions(
            predictiveCacheLocationOptions: PredictiveCacheLocationOptions,
        ): Builder = also { this.predictiveCacheLocationOptions = predictiveCacheLocationOptions }

        /**
         * Minimum zoom level for the tile package.
         * See **com.mapbox.maps.TilesetDescriptorOptions#getMinZoom**
         *
         * Defaults to 15
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
         * Extra tileset descriptor options.
         * See **com.mapbox.maps.TilesetDescriptorOptions#getExtraOptions**
         *
         * Defaults to null.
         */
        fun extraOptions(extraOptions: Value?): Builder = apply {
            this.extraOptions = extraOptions
        }

        /**
         * The tilesets associated with the tileset descriptor.
         * See **com.mapbox.maps.TilesetDescriptorOptions#getTilesets**
         *
         * Defaults to null.
         */
        @ExperimentalPreviewMapboxNavigationAPI
        fun tilesets(tilesets: List<String>?): Builder = apply {
            this.tilesets = tilesets
        }

        /**
         * Build [PredictiveCacheMapsOptions].
         */
        fun build(): PredictiveCacheMapsOptions = PredictiveCacheMapsOptions(
            predictiveCacheLocationOptions ?: PredictiveCacheLocationOptions.Builder().build(),
            minZoom,
            maxZoom,
            extraOptions,
            tilesets,
        )
    }
}
