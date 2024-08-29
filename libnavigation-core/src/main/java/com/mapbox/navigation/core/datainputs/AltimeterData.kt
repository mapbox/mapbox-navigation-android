package com.mapbox.navigation.core.datainputs

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.internal.utils.safeCompareTo

/**
 * Represents altimeter data including pressure and timestamp.
 *
 * @param pressure The atmospheric pressure in kilopascals.
 * @param monotonicTimestampNanoseconds Timestamp which should be in sync with timestamps of
 * locations from location provider.
 */
@ExperimentalPreviewMapboxNavigationAPI
class AltimeterData(
    val pressure: Float,
    val monotonicTimestampNanoseconds: Long,
) {

    @JvmSynthetic
    internal fun mapToNative(): com.mapbox.navigator.AltimeterData {
        return com.mapbox.navigator.AltimeterData(
            pressure,
            monotonicTimestampNanoseconds,
        )
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AltimeterData

        if (!pressure.safeCompareTo(other.pressure)) return false
        return monotonicTimestampNanoseconds == other.monotonicTimestampNanoseconds
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = pressure.hashCode()
        result = 31 * result + monotonicTimestampNanoseconds.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "AltimeterData(" +
            "pressure=$pressure, " +
            "monotonicTimestampNanoseconds=$monotonicTimestampNanoseconds" +
            ")"
    }
}
