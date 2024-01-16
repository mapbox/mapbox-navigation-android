package com.mapbox.navigation.core.adasis

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Configuration of ADASISv2 feature.
 *
 * @param dataSending data sending configuration
 * @param pathOptions ADASISv2 path level specific configurations
 */
@ExperimentalPreviewMapboxNavigationAPI
class AdasisConfig private constructor(
    val dataSending: AdasisConfigDataSending,
    val pathOptions: AdasisConfigPathOptions,
) {

    /**
     * Get a builder to customize a subset of current configuration.
     */
    fun toBuilder(): Builder = Builder()
        .dataSending(dataSending)
        .pathOptions(pathOptions)

    @JvmSynthetic
    internal fun toNativeAdasisConfig(): com.mapbox.navigator.AdasisConfig {
        return com.mapbox.navigator.AdasisConfig(
            dataSending.toNativeAdasisConfigDataSending(),
            pathOptions.toNativeAdasisConfigPathOptions(),
        )
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AdasisConfig

        if (dataSending != other.dataSending) return false
        return pathOptions == other.pathOptions
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = dataSending.hashCode()
        result = 31 * result + pathOptions.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "AdasisConfig(" +
            "dataSending=$dataSending, " +
            "pathOptions=$pathOptions" +
            ")"
    }

    /**
     * Builder for [AdasisConfig]
     */
    class Builder {

        private var dataSending = AdasisConfigDataSending.Builder(
            AdasisMessageBinaryFormat.FlatBuffers
        ).build()

        private var pathOptions = AdasisConfigPathOptions.Builder().build()

        /**
         * Data sending configuration
         */
        fun dataSending(dataSending: AdasisConfigDataSending) = apply {
            this.dataSending = dataSending
        }

        /**
         * ADASISv2 path level specific configurations
         */
        fun pathOptions(pathsOptions: AdasisConfigPathOptions) = apply {
            this.pathOptions = pathsOptions
        }

        /**
         * Build the [AdasisConfig]
         */
        fun build() = AdasisConfig(
            dataSending = dataSending,
            pathOptions = pathOptions,
        )
    }
}
