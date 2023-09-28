package com.mapbox.navigation.core.adasis

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Configuration of ADASISv2 feature.
 *
 * @param cycleTimes message generation cycle times configuration
 * @param dataSending data sending configuration
 * @param pathsOptions ADASISv2 path level specific configurations
 */
@ExperimentalPreviewMapboxNavigationAPI
class AdasisConfig(
    val cycleTimes: AdasisConfigCycleTimes,
    val dataSending: AdasisConfigDataSending,
    val pathsOptions: AdasisConfigPathsConfigs,
) {

    @JvmSynthetic
    internal fun toNativeAdasisConfig(): com.mapbox.navigator.AdasisConfig {
        return com.mapbox.navigator.AdasisConfig(
            cycleTimes.toNativeAdasisConfigCycleTimes(),
            dataSending.toNativeAdasisConfigDataSending(),
            pathsOptions.toNativeAdasisConfigPathsConfigs(),
        )
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AdasisConfig

        if (cycleTimes != other.cycleTimes) return false
        if (dataSending != other.dataSending) return false
        if (pathsOptions != other.pathsOptions) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = cycleTimes.hashCode()
        result = 31 * result + dataSending.hashCode()
        result = 31 * result + pathsOptions.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "AdasisConfig(" +
            "cycleTimes=$cycleTimes, " +
            "dataSending=$dataSending, " +
            "pathsOptions=$pathsOptions" +
            ")"
    }
}
