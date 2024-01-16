package com.mapbox.navigation.core.adasis

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Profile long message options
 *
 * @param options common options for profile long message
 * @param profileOptions options for each type of profile long message
 */
@ExperimentalPreviewMapboxNavigationAPI
class AdasisProfileLongOptions private constructor(
    val options: AdasisConfigMessageOptions,
    val profileOptions: AdasisConfigProfileLongTypeOptions,
) {

    /**
     * Get a builder to customize a subset of current options.
     */
    fun toBuilder(): Builder = Builder()
        .options(options)
        .profileOptions(profileOptions)

    @JvmSynthetic
    internal fun toNativeProfileLong(): com.mapbox.navigator.Profilelong {
        return com.mapbox.navigator.Profilelong(
            options.toNativeAdasisConfigMessageOptions(),
            profileOptions.toNativeAdasisConfigProfileLongTypeOptions()
        )
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AdasisProfileLongOptions

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
        return "ProfileLong(options=$options, profileOptions=$profileOptions)"
    }

    /**
     * Builder for [AdasisProfileLongOptions].
     */
    class Builder {

        private var options = AdasisConfigMessageOptions.Builder().build()
        private var profileOptions = AdasisConfigProfileLongTypeOptions.Builder().build()

        /**
         * Common options for profile long message
         */
        fun options(options: AdasisConfigMessageOptions) = apply {
            this.options = options
        }

        /**
         * Types options for each type of profile long message
         */
        fun profileOptions(profileOptions: AdasisConfigProfileLongTypeOptions) = apply {
            this.profileOptions = profileOptions
        }

        /**
         * Build the [AdasisProfileLongOptions]
         */
        fun build() = AdasisProfileLongOptions(
            options = options,
            profileOptions = profileOptions,
        )
    }
}
