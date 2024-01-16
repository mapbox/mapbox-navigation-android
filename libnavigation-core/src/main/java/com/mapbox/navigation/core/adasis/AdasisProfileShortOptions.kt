package com.mapbox.navigation.core.adasis

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Profile short message options
 *
 * @param options common options for profile short message
 * @param profileOptions options for each type of profile short message
 */
@ExperimentalPreviewMapboxNavigationAPI
class AdasisProfileShortOptions private constructor(
    val options: AdasisConfigMessageOptions,
    val profileOptions: AdasisConfigProfileShortTypeOptions,
) {

    /**
     * Get a builder to customize a subset of current options.
     */
    fun toBuilder(): Builder = Builder()
        .options(options)
        .profileOptions(profileOptions)

    @JvmSynthetic
    internal fun toNativeProfileShort(): com.mapbox.navigator.Profileshort {
        return com.mapbox.navigator.Profileshort(
            options.toNativeAdasisConfigMessageOptions(),
            profileOptions.toNativeAdasisConfigProfileShortTypeOptions(),
        )
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AdasisProfileShortOptions

        if (options != other.options) return false
        return profileOptions == other.profileOptions
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = options.hashCode()
        result = 31 * result + profileOptions.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "ProfileShort(options=$options, profileOptions=$profileOptions)"
    }

    /**
     * Builder for [AdasisProfileShortOptions].
     */
    class Builder {

        private var options = AdasisConfigMessageOptions.Builder().build()
        private var profileOptions = AdasisConfigProfileShortTypeOptions.Builder().build()

        /**
         * Common options for profile long message
         */
        fun options(options: AdasisConfigMessageOptions) = apply {
            this.options = options
        }

        /**
         * Types options for each type of profile short message
         */
        fun profileOptions(profileOptions: AdasisConfigProfileShortTypeOptions) = apply {
            this.profileOptions = profileOptions
        }

        /**
         * Build the [AdasisProfileShortOptions]
         */
        fun build() = AdasisProfileShortOptions(
            options = options,
            profileOptions = profileOptions,
        )
    }
}
