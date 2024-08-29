package com.mapbox.navigation.core.datainputs

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.geometry.Angle
import com.mapbox.navigation.base.geometry.AngleUnit
import com.mapbox.navigation.base.internal.utils.safeCompareTo

/**
 * Represents raw GNSS satellite data, which includes various metrics related to the satellite's signal.
 *
 * @param svid The Satellite vehicle ID.
 * @param carrierFrequencyHz The carrier frequency of the satellite signal in Hz, or null if not available.
 * @param basebandCn0DbHz The baseband carrier-to-noise density in dB-Hz, or null if not available.
 * @param cn0DbHz The Carrier-to-noise density in dB-Hz.
 * @param usedInFix Indicates whether the specific satellite has been seen and used
 * in the calculation of the location (satellite in use).
 * @param hasEphemerisData Indicates whether ephemeris data is available for the satellite.
 * @param hasAlmanacData Indicates whether almanac data is available for the satellite.
 * @param constellationType The constellation type of the satellite (e.g., GPS, GALILEO, etc.).
 * @param azimuth The azimuth of the satellite.
 * @param elevation The elevation of the satellite.
 */
@ExperimentalPreviewMapboxNavigationAPI
class RawGnssSatelliteData(
    val svid: Int,
    val carrierFrequencyHz: Float?,
    val basebandCn0DbHz: Double?,
    val cn0DbHz: Double,
    val usedInFix: Boolean,
    val hasEphemerisData: Boolean,
    val hasAlmanacData: Boolean,
    val constellationType: ConstellationType,
    val azimuth: Angle,
    val elevation: Angle,
) {

    @JvmSynthetic
    internal fun mapToNative(): com.mapbox.navigator.RawGnssSatelliteData {
        return com.mapbox.navigator.RawGnssSatelliteData(
            svid,
            carrierFrequencyHz,
            basebandCn0DbHz,
            cn0DbHz,
            usedInFix,
            hasEphemerisData,
            hasAlmanacData,
            constellationType.nativeType,
            azimuth.toFloat(AngleUnit.DEGREES),
            elevation.toFloat(AngleUnit.DEGREES),
        )
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RawGnssSatelliteData

        if (svid != other.svid) return false
        if (!carrierFrequencyHz.safeCompareTo(other.carrierFrequencyHz)) return false
        if (!basebandCn0DbHz.safeCompareTo(other.basebandCn0DbHz)) return false
        if (!cn0DbHz.safeCompareTo(other.cn0DbHz)) return false
        if (usedInFix != other.usedInFix) return false
        if (hasEphemerisData != other.hasEphemerisData) return false
        if (hasAlmanacData != other.hasAlmanacData) return false
        if (constellationType != other.constellationType) return false
        if (azimuth != other.azimuth) return false
        return elevation == other.elevation
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = svid
        result = 31 * result + (carrierFrequencyHz?.hashCode() ?: 0)
        result = 31 * result + (basebandCn0DbHz?.hashCode() ?: 0)
        result = 31 * result + cn0DbHz.hashCode()
        result = 31 * result + usedInFix.hashCode()
        result = 31 * result + hasEphemerisData.hashCode()
        result = 31 * result + hasAlmanacData.hashCode()
        result = 31 * result + constellationType.hashCode()
        result = 31 * result + azimuth.hashCode()
        result = 31 * result + elevation.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "RawGnssSatelliteData(" +
            "svid=$svid, " +
            "carrierFrequencyHz=$carrierFrequencyHz, " +
            "basebandCn0DbHz=$basebandCn0DbHz, " +
            "cn0DbHz=$cn0DbHz, " +
            "usedInFix=$usedInFix, " +
            "hasEphemerisData=$hasEphemerisData, " +
            "hasAlmanacData=$hasAlmanacData, " +
            "constellationType=$constellationType, " +
            "azimuth=$azimuth, " +
            "elevation=$elevation" +
            ")"
    }
}
