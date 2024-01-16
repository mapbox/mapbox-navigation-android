package com.mapbox.navigation.core.adasis

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Profile long message options
 *
 * @param options common options for profile long message
 * @param types options for each type of profile long message
 */
@ExperimentalPreviewMapboxNavigationAPI
class ProfileLong private constructor(
    val options: AdasisConfigMessageOptions,
    val types: AdasisConfigProfileLongTypeOptions,
) {

    /**
     * Get a builder to customize a subset of current options.
     */
    fun toBuilder(): Builder = Builder()
        .options(options)
        .types(types)

    @JvmSynthetic
    internal fun toNativeProfileLong(): com.mapbox.navigator.Profilelong {
        return com.mapbox.navigator.Profilelong(
            options.toNativeAdasisConfigMessageOptions(),
            types.toNativeAdasisConfigProfileLongTypeOptions()
        )
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ProfileLong

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
        return "ProfileLong(options=$options, types=$types)"
    }

    /**
     * Builder for [ProfileLong].
     */
    class Builder {

        private var options = AdasisConfigMessageOptions.Builder().build()
        private var types = AdasisConfigProfileLongTypeOptions.Builder().build()

        /**
         * Common options for profile long message
         */
        fun options(options: AdasisConfigMessageOptions) = apply {
            this.options = options
        }

        /**
         * Types options for each type of profile long message
         */
        fun types(types: AdasisConfigProfileLongTypeOptions) = apply {
            this.types = types
        }

        /**
         * Build the [ProfileLong]
         */
        fun build() = ProfileLong(
            options = options,
            types = types,
        )
    }
}
