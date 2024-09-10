package com.mapbox.navigation.core.adas

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Common message options
 * @param enable if true, message of that type will be generated
 * @param radiusMeters distance along EH path in meters, for which message will be generated
 */
@ExperimentalPreviewMapboxNavigationAPI
internal class AdasisConfigMessageOptions private constructor(
    val enable: Boolean,
    val radiusMeters: Int,
) {

    /**
     * Get a builder to customize a subset of current options.
     */
    fun toBuilder(): Builder = Builder()
        .enable(enable)
        .radiusMeters(radiusMeters)

    @JvmSynthetic
    internal fun toNativeAdasisConfigMessageOptions():
        com.mapbox.navigator.AdasisConfigMessageOptions {
        return com.mapbox.navigator.AdasisConfigMessageOptions(
            enable,
            radiusMeters,
        )
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AdasisConfigMessageOptions

        if (enable != other.enable) return false
        return radiusMeters == other.radiusMeters
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = enable.hashCode()
        result = 31 * result + radiusMeters
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "AdasisConfigMessageOptions(" +
            "enable=$enable, " +
            "radiusMeters=$radiusMeters" +
            ")"
    }

    /**
     * Builder for [AdasisConfigMessageOptions].
     */
    class Builder {

        private var enable: Boolean = true
        private var radiusMeters: Int = 2000

        /**
         * If true, message of that type will be generated
         */
        fun enable(enable: Boolean): Builder = apply {
            this.enable = enable
        }

        /**
         * Distance along EH path in meters, for which message will be generated
         */
        fun radiusMeters(radiusMeters: Int): Builder = apply {
            this.radiusMeters = radiusMeters
        }

        /**
         * Build the [AdasisConfigMessageOptions]
         */
        fun build() = AdasisConfigMessageOptions(
            enable = enable,
            radiusMeters = radiusMeters,
        )
    }
}
