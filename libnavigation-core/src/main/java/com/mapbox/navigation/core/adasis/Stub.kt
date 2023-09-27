package com.mapbox.navigation.core.adasis

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Stub message options
 *
 * @param options common options for segment
 */
@ExperimentalPreviewMapboxNavigationAPI
class Stub(
    val options: AdasisConfigMessageOptions,
) {

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
        return "Stub(options=$options)"
    }
}
