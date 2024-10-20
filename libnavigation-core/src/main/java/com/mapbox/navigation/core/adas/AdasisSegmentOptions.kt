package com.mapbox.navigation.core.adas

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Segment message options
 *
 * @param options common options for segment
 */
@ExperimentalPreviewMapboxNavigationAPI
internal class AdasisSegmentOptions private constructor(
    val options: AdasisConfigMessageOptions,
) {

    /**
     * Get a builder to customize a subset of current options.
     */
    fun toBuilder(): Builder = Builder()
        .options(options)

    @JvmSynthetic
    internal fun toNativeSegment(): com.mapbox.navigator.Segment {
        return com.mapbox.navigator.Segment(
            options.toNativeAdasisConfigMessageOptions(),
        )
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AdasisSegmentOptions

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
        return "Segment(options=$options)"
    }

    /**
     * Builder for [AdasisSegmentOptions].
     */
    class Builder {

        private var options = AdasisConfigMessageOptions.Builder().build()

        /**
         * Common options for segment
         */
        fun options(options: AdasisConfigMessageOptions): Builder = apply {
            this.options = options
        }

        /**
         * Build the [AdasisSegmentOptions]
         */
        fun build() = AdasisSegmentOptions(options)
    }
}
