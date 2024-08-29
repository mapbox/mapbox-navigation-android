package com.mapbox.navigation.core.adas

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Profile Long message options
 *
 * @param lat if true, latitude type will be generated
 * @param lon if true, longitude type will be generated
 * @param trafficSign if true, Traffic Sign type will be generated
 */
@ExperimentalPreviewMapboxNavigationAPI
class AdasisConfigProfileLongTypeOptions private constructor(
    val lat: Boolean,
    val lon: Boolean,
    val trafficSign: Boolean,
) {

    /**
     * Get a builder to customize a subset of current options.
     */
    fun toBuilder(): Builder = Builder()
        .lat(lat)
        .lon(lon)
        .trafficSign(trafficSign)

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
        return trafficSign == other.trafficSign
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

    /**
     * Builder for [AdasisConfigProfileLongTypeOptions].
     */
    class Builder {

        private var lat: Boolean = true
        private var lon: Boolean = true
        private var trafficSign: Boolean = false

        /**
         * If true, latitude type will be generated
         */
        fun lat(lat: Boolean): Builder = apply {
            this.lat = lat
        }

        /**
         * If true, longitude type will be generated
         */
        fun lon(lon: Boolean): Builder = apply {
            this.lon = lon
        }

        /**
         * If true, Traffic Sign type will be generated
         */
        fun trafficSign(trafficSign: Boolean): Builder = apply {
            this.trafficSign = trafficSign
        }

        /**
         * Build the [AdasisConfigProfileLongTypeOptions]
         */
        fun build() = AdasisConfigProfileLongTypeOptions(
            lat = lat,
            lon = lon,
            trafficSign = trafficSign,
        )
    }
}
