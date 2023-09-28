package com.mapbox.navigation.core.adasis

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * ADASISv2 path level specific configurations
 *
 * @param mpp most probable path config
 * @param level1 level 1 path config
 * @param level2 level 2 path config
 */
@ExperimentalPreviewMapboxNavigationAPI
class AdasisConfigPathsConfigs(
    val mpp: AdasisConfigPathLevelOptions,
    val level1: AdasisConfigPathLevelOptions,
    val level2: AdasisConfigPathLevelOptions,
) {

    @JvmSynthetic
    internal fun toNativeAdasisConfigPathsConfigs(): com.mapbox.navigator.AdasisConfigPathsConfigs {
        return com.mapbox.navigator.AdasisConfigPathsConfigs(
            mpp.toNativeAdasisConfigPathLevelOptions(),
            level1.toNativeAdasisConfigPathLevelOptions(),
            level2.toNativeAdasisConfigPathLevelOptions(),
        )
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AdasisConfigPathsConfigs

        if (mpp != other.mpp) return false
        if (level1 != other.level1) return false
        if (level2 != other.level2) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = mpp.hashCode()
        result = 31 * result + level1.hashCode()
        result = 31 * result + level2.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "AdasisConfigPathsConfigs(mpp=$mpp, level1=$level1, level2=$level2)"
    }
}
