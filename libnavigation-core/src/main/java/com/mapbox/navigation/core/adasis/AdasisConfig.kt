package com.mapbox.navigation.core.adasis

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Configuration of ADASISv2 feature.
 *
 * @param dataSendingConfig data sending configuration
 * @param pathOptions ADASISv2 path level specific configurations
 */
@ExperimentalPreviewMapboxNavigationAPI
class AdasisConfig private constructor(
    val dataSendingConfig: AdasisDataSendingConfig,
    val pathOptions: AdasisConfigPathOptions,
) {

    /**
     * Get a builder to customize a subset of current configuration.
     */
    fun toBuilder(): Builder = Builder()
        .dataSendingConfig(dataSendingConfig)
        .pathOptions(pathOptions)

    @JvmSynthetic
    internal fun toNativeAdasisConfig(): com.mapbox.navigator.AdasisConfig {
        return com.mapbox.navigator.AdasisConfig(
            dataSendingConfig.toNativeAdasisConfigDataSending(),
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

        if (dataSendingConfig != other.dataSendingConfig) return false
        return pathOptions == other.pathOptions
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = dataSendingConfig.hashCode()
        result = 31 * result + pathOptions.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "AdasisConfig(" +
            "dataSendingConfig=$dataSendingConfig, " +
            "pathOptions=$pathOptions" +
            ")"
    }

    /**
     * Builder for [AdasisConfig]
     */
    class Builder {

        private var dataSendingConfig = AdasisDataSendingConfig.Builder(
            AdasisMessageBinaryFormat.FlatBuffers
        ).build()

        private var pathOptions = AdasisConfigPathOptions.Builder().build()

        /**
         * Data sending configuration
         */
        fun dataSendingConfig(dataSending: AdasisDataSendingConfig) = apply {
            this.dataSendingConfig = dataSending
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
            dataSendingConfig = dataSendingConfig,
            pathOptions = pathOptions,
        )
    }
}
