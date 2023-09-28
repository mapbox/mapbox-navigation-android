package com.mapbox.navigation.core.adasis

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Profile short message options
 *
 * @param options common options for profile short message
 * @param types options for each type of profile short message
 */
@ExperimentalPreviewMapboxNavigationAPI
class ProfileShort(
    val options: AdasisConfigMessageOptions,
    val types: AdasisConfigProfileShortTypeOptions,
) {

    @JvmSynthetic
    internal fun toNativeProfileShort(): com.mapbox.navigator.Profileshort {
        return com.mapbox.navigator.Profileshort(
            options.toNativeAdasisConfigMessageOptions(),
            types.toNativeAdasisConfigProfileShortTypeOptions(),
        )
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ProfileShort

        if (options != other.options) return false
        if (types != other.types) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = options.hashCode()
        result = 31 * result + types.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "ProfileShort(options=$options, types=$types)"
    }
}
