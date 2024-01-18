package com.mapbox.navigation.core.adas

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Stub message options
 *
 * @param options common options for stub
 */
@ExperimentalPreviewMapboxNavigationAPI
class AdasisStubOptions private constructor(
    val options: AdasisConfigMessageOptions,
) {

    /**
     * Get a builder to customize a subset of current options.
     */
    fun toBuilder(): Builder = Builder()
        .options(options)

    @JvmSynthetic
    internal fun toNativeStub(): com.mapbox.navigator.Stub {
        return com.mapbox.navigator.Stub(
            options.toNativeAdasisConfigMessageOptions()
        )
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AdasisStubOptions

        return options == other.options
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        return options.hashCode()
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "Stub(options=$options)"
    }

    /**
     * Builder for [AdasisStubOptions].
     */
    class Builder {

        private var options = AdasisConfigMessageOptions.Builder().build()

        /**
         * Common options for segment
         */
        fun options(options: AdasisConfigMessageOptions) = apply {
            this.options = options
        }

        /**
         * Build the [AdasisStubOptions]
         */
        fun build() = AdasisStubOptions(options)
    }
}
