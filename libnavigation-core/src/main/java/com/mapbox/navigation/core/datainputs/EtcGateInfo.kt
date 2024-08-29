package com.mapbox.navigation.core.datainputs

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigator.ETCGateInfo

/**
 * Class containing information about ETC gate.
 * @param id The gate id.
 * @param monotonicTimestampNanoseconds Timestamp which should be in sync with timestamps of
 * locations from location provider.
 */
@ExperimentalPreviewMapboxNavigationAPI
class EtcGateInfo(
    val id: Int,
    val monotonicTimestampNanoseconds: Long,
) {

    @JvmSynthetic
    internal fun mapToNative(): ETCGateInfo {
        return ETCGateInfo(id, monotonicTimestampNanoseconds)
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EtcGateInfo

        if (id != other.id) return false
        return monotonicTimestampNanoseconds == other.monotonicTimestampNanoseconds
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = id
        result = 31 * result + monotonicTimestampNanoseconds.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "EtcGateInfo(id=$id, monotonicTimestampNanoseconds=$monotonicTimestampNanoseconds)"
    }
}
