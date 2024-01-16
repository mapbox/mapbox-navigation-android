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
class AdasisConfigPathOptions private constructor(
    val stub: Stub,
    val segment: Segment,
    val profileShort: ProfileShort,
    val profileLong: ProfileLong,
) {

    /**
     * Get a builder to customize a subset of current options.
     */
    fun toBuilder(): Builder = Builder()
        .stub(stub)
        .segment(segment)
        .profileShort(profileShort)
        .profileLong(profileLong)

    @JvmSynthetic
    internal fun toNativeAdasisConfigPathOptions():
        com.mapbox.navigator.AdasisConfigPathOptions {
        return com.mapbox.navigator.AdasisConfigPathOptions(
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

        other as AdasisConfigPathOptions

        if (stub != other.stub) return false
        if (segment != other.segment) return false
        if (profileShort != other.profileShort) return false
        return profileLong == other.profileLong
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

    /**
     * Builder for [AdasisConfigPathOptions].
     */
    class Builder {

        private var stub = Stub.Builder().build()
        private var segment = Segment.Builder().build()
        private var profileShort = ProfileShort.Builder().build()
        private var profileLong = ProfileLong.Builder().build()

        /**
         * Stub message options
         */
        fun stub(stub: Stub) = apply {
            this.stub = stub
        }

        /**
         * Segment message options
         */
        fun segment(segment: Segment) = apply {
            this.segment = segment
        }

        /**
         * Profile short message options
         */
        fun profileShort(profileShort: ProfileShort) = apply {
            this.profileShort = profileShort
        }

        /**
         * ProfileLong Profile long message options
         */
        fun profileLong(profileLong: ProfileLong) = apply {
            this.profileLong = profileLong
        }

        /**
         * Build the [AdasisConfigPathOptions]
         */
        fun build() = AdasisConfigPathOptions(
            stub = stub,
            segment = segment,
            profileShort = profileShort,
            profileLong = profileLong,
        )
    }
}
