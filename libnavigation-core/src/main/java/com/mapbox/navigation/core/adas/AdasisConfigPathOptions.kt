package com.mapbox.navigation.core.adas

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Path level options
 *
 * @param stubOptions Stub message options
 * @param segmentOptions Segment message options
 * @param profileShortOptions Profile short message options
 * @param profileLongOptions Profile long message options
 */
@ExperimentalPreviewMapboxNavigationAPI
class AdasisConfigPathOptions private constructor(
    val stubOptions: AdasisStubOptions,
    val segmentOptions: AdasisSegmentOptions,
    val profileShortOptions: AdasisProfileShortOptions,
    val profileLongOptions: AdasisProfileLongOptions,
) {

    /**
     * Get a builder to customize a subset of current options.
     */
    fun toBuilder(): Builder = Builder()
        .stubOptions(stubOptions)
        .segmentOptions(segmentOptions)
        .profileShortOptions(profileShortOptions)
        .profileLongOptions(profileLongOptions)

    @JvmSynthetic
    internal fun toNativeAdasisConfigPathOptions():
        com.mapbox.navigator.AdasisConfigPathOptions {
        return com.mapbox.navigator.AdasisConfigPathOptions(
            stubOptions.toNativeStub(),
            segmentOptions.toNativeSegment(),
            profileShortOptions.toNativeProfileShort(),
            profileLongOptions.toNativeProfileLong(),
        )
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AdasisConfigPathOptions

        if (stubOptions != other.stubOptions) return false
        if (segmentOptions != other.segmentOptions) return false
        if (profileShortOptions != other.profileShortOptions) return false
        return profileLongOptions == other.profileLongOptions
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = stubOptions.hashCode()
        result = 31 * result + segmentOptions.hashCode()
        result = 31 * result + profileShortOptions.hashCode()
        result = 31 * result + profileLongOptions.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "AdasisConfigPathLevelOptions(" +
            "stubOptions=$stubOptions, " +
            "segmentOptions=$segmentOptions, " +
            "profileShortOptions=$profileShortOptions, " +
            "profileLongOptions=$profileLongOptions" +
            ")"
    }

    /**
     * Builder for [AdasisConfigPathOptions].
     */
    class Builder {

        private var stubOptions = AdasisStubOptions.Builder().build()
        private var segmentOptions = AdasisSegmentOptions.Builder().build()
        private var profileShortOptions = AdasisProfileShortOptions.Builder().build()
        private var profileLongOptions = AdasisProfileLongOptions.Builder().build()

        /**
         * Stub message options
         */
        fun stubOptions(stubOptions: AdasisStubOptions): Builder = apply {
            this.stubOptions = stubOptions
        }

        /**
         * Segment message options
         */
        fun segmentOptions(segmentOptions: AdasisSegmentOptions): Builder = apply {
            this.segmentOptions = segmentOptions
        }

        /**
         * Profile short message options
         */
        fun profileShortOptions(profileShortOptions: AdasisProfileShortOptions): Builder = apply {
            this.profileShortOptions = profileShortOptions
        }

        /**
         * ProfileLong Profile long message options
         */
        fun profileLongOptions(profileLongOptions: AdasisProfileLongOptions): Builder = apply {
            this.profileLongOptions = profileLongOptions
        }

        /**
         * Build the [AdasisConfigPathOptions]
         */
        fun build() = AdasisConfigPathOptions(
            stubOptions = stubOptions,
            segmentOptions = segmentOptions,
            profileShortOptions = profileShortOptions,
            profileLongOptions = profileLongOptions,
        )
    }
}
