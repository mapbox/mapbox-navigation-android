package com.mapbox.navigation.core.adasis

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Path level options
 *
 * @param stub Stub message options
 * @param segment Segment message options
 * @param profileShort Profile short message options
 * @param profileLong Profile long message options
 */
@ExperimentalPreviewMapboxNavigationAPI
class AdasisConfigPathLevelOptions(
    val stub: Stub,
    val segment: Segment,
    val profileShort: ProfileShort,
    val profileLong: ProfileLong,
) {

    @JvmSynthetic
    internal fun toNativeAdasisConfigPathLevelOptions():
        com.mapbox.navigator.AdasisConfigPathLevelOptions {
        return com.mapbox.navigator.AdasisConfigPathLevelOptions(
            stub.toNativeStub(),
            segment.toNativeSegment(),
            profileShort.toNativeProfileShort(),
            profileLong.toNativeProfileLong(),
        )
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AdasisConfigPathLevelOptions

        if (stub != other.stub) return false
        if (segment != other.segment) return false
        if (profileShort != other.profileShort) return false
        if (profileLong != other.profileLong) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = stub.hashCode()
        result = 31 * result + segment.hashCode()
        result = 31 * result + profileShort.hashCode()
        result = 31 * result + profileLong.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "AdasisConfigPathLevelOptions(" +
            "stub=$stub, " +
            "segment=$segment, " +
            "profileShort=$profileShort, " +
            "profileLong=$profileLong" +
            ")"
    }
}
