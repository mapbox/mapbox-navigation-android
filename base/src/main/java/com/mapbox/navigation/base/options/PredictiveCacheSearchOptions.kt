package com.mapbox.navigation.base.options

import com.mapbox.common.TilesetDescriptor

/**
 * Predictive cache search related options. Only Search TilesetDescriptor should be passed here (any other type won't work).
 * See the following documentation for how to create a Search TilesetDescriptor: [https://docs.mapbox.com/android/search/guides/search-engine/offline/#define-a-tileset-descriptors-and-load-tile-regions]
 *
 * @param predictiveCacheLocationOptions [PredictiveCacheLocationOptions] location configuration for visual map predictive caching
 * @param searchTilesetDescriptor The descriptor of the search tileset to download
 */
class PredictiveCacheSearchOptions private constructor(
    val predictiveCacheLocationOptions: PredictiveCacheLocationOptions,
    val searchTilesetDescriptor: TilesetDescriptor,
) {

    /**
     * Get a builder to customize a subset of current options.
     */
    fun toBuilder(): Builder = Builder(searchTilesetDescriptor).apply {
        predictiveCacheLocationOptions(predictiveCacheLocationOptions)
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PredictiveCacheSearchOptions

        if (predictiveCacheLocationOptions != other.predictiveCacheLocationOptions) return false
        return searchTilesetDescriptor == other.searchTilesetDescriptor
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = predictiveCacheLocationOptions.hashCode()
        result = 31 * result + searchTilesetDescriptor.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "PredictiveCacheSearchOptions(" +
            "predictiveCacheLocationOptions=$predictiveCacheLocationOptions, " +
            "searchTilesetDescriptor=$searchTilesetDescriptor" +
            ")"
    }

    /**
     * Build a new [PredictiveCacheSearchOptions].
     * @param searchTilesetDescriptor The descriptor of the search tileset to download
     */
    class Builder(private val searchTilesetDescriptor: TilesetDescriptor) {

        private var predictiveCacheLocationOptions: PredictiveCacheLocationOptions =
            PredictiveCacheLocationOptions.Builder().build()

        /**
         * Location configuration for predictive caching
         */
        fun predictiveCacheLocationOptions(
            predictiveCacheLocationOptions: PredictiveCacheLocationOptions,
        ): Builder = also { this.predictiveCacheLocationOptions = predictiveCacheLocationOptions }

        /**
         * Build [PredictiveCacheSearchOptions].
         */
        fun build(): PredictiveCacheSearchOptions =
            PredictiveCacheSearchOptions(
                predictiveCacheLocationOptions,
                searchTilesetDescriptor,
            )
    }
}
