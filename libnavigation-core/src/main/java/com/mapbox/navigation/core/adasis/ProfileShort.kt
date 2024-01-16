package com.mapbox.navigation.core.adasis

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Profile short message options
 *
 * @param options common options for profile short message
 * @param types options for each type of profile short message
 */
@ExperimentalPreviewMapboxNavigationAPI
class ProfileShort private constructor(
    val options: AdasisConfigMessageOptions,
    val types: AdasisConfigProfileShortTypeOptions,
) {

    /**
     * Get a builder to customize a subset of current options.
     */
    fun toBuilder(): Builder = Builder()
        .options(options)
        .types(types)

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
        return types == other.types
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

    /**
     * Builder for [ProfileShort].
     */
    class Builder {

        private var options = AdasisConfigMessageOptions.Builder().build()
        private var types = AdasisConfigProfileShortTypeOptions.Builder().build()

        /**
         * Common options for profile long message
         */
        fun options(options: AdasisConfigMessageOptions) = apply {
            this.options = options
        }

        /**
         * Types options for each type of profile short message
         */
        fun types(types: AdasisConfigProfileShortTypeOptions) = apply {
            this.types = types
        }

        /**
         * Build the [ProfileShort]
         */
        fun build() = ProfileShort(
            options = options,
            types = types,
        )
    }
}
