package com.mapbox.navigation.core.adasis

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Profile Long message options
 *
 * @param lat if true, latitude type will be generated
 * @param lon if true, longitude type will be generated
 * @param trafficSign if true, Traffic Sign type will be generated
 */
@ExperimentalPreviewMapboxNavigationAPI
class AdasisConfigProfileLongTypeOptions(
    val lat: Boolean = true,
    val lon: Boolean = true,
    val trafficSign: Boolean = false,
) {

    @JvmSynthetic
    internal fun toNativeAdasisConfigProfileLongTypeOptions():
        com.mapbox.navigator.AdasisConfigProfilelongTypeOptions {
        return com.mapbox.navigator.AdasisConfigProfilelongTypeOptions(
            lat,
            lon,
            trafficSign,
        )
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AdasisConfigProfileLongTypeOptions

        if (lat != other.lat) return false
        if (lon != other.lon) return false
        if (trafficSign != other.trafficSign) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = lat.hashCode()
        result = 31 * result + lon.hashCode()
        result = 31 * result + trafficSign.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "AdasisConfigProfileLongTypeOptions(" +
            "lat=$lat, " +
            "lon=$lon, " +
            "trafficSign=$trafficSign, " +
            ")"
    }
}
