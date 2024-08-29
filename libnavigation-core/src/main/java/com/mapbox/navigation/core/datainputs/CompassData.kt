package com.mapbox.navigation.core.datainputs

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.geometry.Angle
import com.mapbox.navigation.base.geometry.AngleUnit
import com.mapbox.navigation.base.geometry.Point3D
import com.mapbox.navigation.base.internal.mapToNative

/**
 * Represents compass data including heading information and raw geomagnetic data.
 *
 * @param magneticHeading The magnetic heading (0 degrees is magnetic North).
 * Null if value is invalid/unknown.
 * @param trueHeading The true heading (0 degrees is true North).
 * Null if value is invalid/unknown.
 * @param headingAccuracy The maximum deviation of where the magnetic heading may differ
 * from the actual geomagnetic heading. Null if value is invalid/unknown.
 * @param rawGeomagneticData The raw geomagnetic data as a 3-dimensional point.
 * @param monotonicTimestampNanoseconds Timestamp which should be in sync with timestamps of
 * locations from location provider.
 */
@ExperimentalPreviewMapboxNavigationAPI
class CompassData(
    val magneticHeading: Angle?,
    val trueHeading: Angle?,
    val headingAccuracy: Angle?,
    val rawGeomagneticData: Point3D,
    val monotonicTimestampNanoseconds: Long,
) {

    @JvmSynthetic
    internal fun mapToNative(): com.mapbox.navigator.CompassData {
        return com.mapbox.navigator.CompassData(
            magneticHeading?.toFloat(AngleUnit.DEGREES),
            trueHeading?.toFloat(AngleUnit.DEGREES),
            headingAccuracy?.toFloat(AngleUnit.DEGREES),
            rawGeomagneticData.mapToNative(),
            monotonicTimestampNanoseconds,
        )
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CompassData

        if (magneticHeading != other.magneticHeading) return false
        if (trueHeading != other.trueHeading) return false
        if (headingAccuracy != other.headingAccuracy) return false
        if (rawGeomagneticData != other.rawGeomagneticData) return false
        return monotonicTimestampNanoseconds == other.monotonicTimestampNanoseconds
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = magneticHeading?.hashCode() ?: 0
        result = 31 * result + (trueHeading?.hashCode() ?: 0)
        result = 31 * result + (headingAccuracy?.hashCode() ?: 0)
        result = 31 * result + rawGeomagneticData.hashCode()
        result = 31 * result + monotonicTimestampNanoseconds.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "CompassData(" +
            "magneticHeading=$magneticHeading, " +
            "trueHeading=$trueHeading, " +
            "headingAccuracy=$headingAccuracy, " +
            "rawGeomagneticData=$rawGeomagneticData, " +
            "monotonicTimestampNanoseconds=$monotonicTimestampNanoseconds" +
            ")"
    }
}
