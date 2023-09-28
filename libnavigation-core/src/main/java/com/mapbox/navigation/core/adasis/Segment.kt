package com.mapbox.navigation.core.adasis

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Segment message options
 *
 * @param options common options for segment
 */
@ExperimentalPreviewMapboxNavigationAPI
class Segment(
    val options: AdasisConfigMessageOptions,
) {

    @JvmSynthetic
    internal fun toNativeSegment(): com.mapbox.navigator.Segment {
        return com.mapbox.navigator.Segment(
            options.toNativeAdasisConfigMessageOptions()
        )
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Segment

        if (options != other.options) return false

        return true
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
}
