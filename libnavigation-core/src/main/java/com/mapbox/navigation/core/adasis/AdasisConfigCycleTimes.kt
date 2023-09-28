package com.mapbox.navigation.core.adasis

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Message generation cycle times configuration
 *
 * @param metadataCycleOnStartMs time in milliseconds between sending metadata message on start
 * @param metadataCycleSeconds time in seconds between repetition of metadata message
 * @param positionCycleMs time in milliseconds between sending position message
 */
@ExperimentalPreviewMapboxNavigationAPI
class AdasisConfigCycleTimes(
    val metadataCycleOnStartMs: Int = 1000,
    val metadataCycleSeconds: Int = 5,
    val positionCycleMs: Int = 200,
) {

    @JvmSynthetic
    internal fun toNativeAdasisConfigCycleTimes(): com.mapbox.navigator.AdasisConfigCycleTimes {
        return com.mapbox.navigator.AdasisConfigCycleTimes(
            metadataCycleOnStartMs,
            metadataCycleSeconds,
            positionCycleMs
        )
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AdasisConfigCycleTimes

        if (metadataCycleOnStartMs != other.metadataCycleOnStartMs) return false
        if (metadataCycleSeconds != other.metadataCycleSeconds) return false
        if (positionCycleMs != other.positionCycleMs) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = metadataCycleOnStartMs
        result = 31 * result + metadataCycleSeconds
        result = 31 * result + positionCycleMs
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "AdasisConfigCycleTimes(" +
            "metadataCycleOnStartMs=$metadataCycleOnStartMs, " +
            "metadataCycleSeconds=$metadataCycleSeconds, " +
            "positionCycleMs=$positionCycleMs" +
            ")"
    }
}
