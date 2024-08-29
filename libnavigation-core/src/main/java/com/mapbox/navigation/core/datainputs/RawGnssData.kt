package com.mapbox.navigation.core.datainputs

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Represents raw GNSS data, including location information and satellite data.
 *
 * @param location The raw GNSS location data.
 * @param satellites The list of raw GNSS satellite data.
 * @param monotonicTimestampNanoseconds Timestamp which should be in sync with timestamps of
 * locations from location provider.
 */
@ExperimentalPreviewMapboxNavigationAPI
class RawGnssData(
    val location: RawGnssLocation,
    val satellites: List<RawGnssSatelliteData>,
    val monotonicTimestampNanoseconds: Long,
) {

    @JvmSynthetic
    internal fun mapToNative(): com.mapbox.navigator.RawGnssData {
        return com.mapbox.navigator.RawGnssData(
            location.mapToNative(),
            satellites.map { it.mapToNative() },
            monotonicTimestampNanoseconds,
        )
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RawGnssData

        if (location != other.location) return false
        if (satellites != other.satellites) return false
        return monotonicTimestampNanoseconds == other.monotonicTimestampNanoseconds
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = location.hashCode()
        result = 31 * result + satellites.hashCode()
        result = 31 * result + monotonicTimestampNanoseconds.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "RawGnssData(" +
            "location=$location, " +
            "satellites=$satellites, " +
            "monotonicTimestampNanoseconds=$monotonicTimestampNanoseconds" +
            ")"
    }
}
