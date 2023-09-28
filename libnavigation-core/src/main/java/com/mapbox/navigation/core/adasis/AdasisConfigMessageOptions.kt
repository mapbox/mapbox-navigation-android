package com.mapbox.navigation.core.adasis

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Common message options
 * @param enable if true, message of that type will be generated
 * @param radiusMeters distance along EH path in meters, for which message will be generated
 * @param repetitionMeters distance along EH path in meters, for which message will be retransmitted
 */
@ExperimentalPreviewMapboxNavigationAPI
class AdasisConfigMessageOptions(
    val enable: Boolean,
    val radiusMeters: Int,
    val repetitionMeters: Int,
) {

    @JvmSynthetic
    internal fun toNativeAdasisConfigMessageOptions():
        com.mapbox.navigator.AdasisConfigMessageOptions {
        return com.mapbox.navigator.AdasisConfigMessageOptions(
            enable,
            radiusMeters,
            repetitionMeters
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
        if (radiusMeters != other.radiusMeters) return false
        if (repetitionMeters != other.repetitionMeters) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = enable.hashCode()
        result = 31 * result + radiusMeters
        result = 31 * result + repetitionMeters
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "AdasisConfigMessageOptions(" +
            "enable=$enable, " +
            "radiusMeters=$radiusMeters, " +
            "repetitionMeters=$repetitionMeters" +
            ")"
    }
}
