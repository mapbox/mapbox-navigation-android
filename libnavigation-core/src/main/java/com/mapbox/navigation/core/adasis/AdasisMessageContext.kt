package com.mapbox.navigation.core.adasis

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Additional context with metadata related to the current messages package
 *
 * @param positionMonotonicTimestampNanoseconds Timestamp has value if Position Message
 * is presented in current message package. Timestamp reflects the time of Position Message
 * creation. Position message creation timestamp is defined by the following variables: previous
 * UpdateLocation.monotonicTimestampNanoseconds increased by the NavigatorConfig.polling.lookAhead
 * value. Meaning that if lookAhead is set to 0, then positionMonotonicTimestampNanoseconds
 * is equal to the location timestamp provided on the previous UpdateLocation call.
 */
@ExperimentalPreviewMapboxNavigationAPI
class AdasisMessageContext private constructor(
    val positionMonotonicTimestampNanoseconds: Long?
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AdasisMessageContext

        return positionMonotonicTimestampNanoseconds == other.positionMonotonicTimestampNanoseconds
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        return positionMonotonicTimestampNanoseconds?.hashCode() ?: 0
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "AdasisMessageContext(positionMonotonicTimestampNanoseconds=$positionMonotonicTimestampNanoseconds)"
    }

    internal companion object {

        @JvmSynthetic
        fun createFromNativeObject(nativeObj: com.mapbox.navigator.AdasisMessageContext) =
            AdasisMessageContext(nativeObj.positionMonotonicTimestampNanoseconds)
    }
}
