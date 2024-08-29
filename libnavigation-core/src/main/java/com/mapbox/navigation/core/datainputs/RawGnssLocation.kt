package com.mapbox.navigation.core.datainputs

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.geometry.Angle
import com.mapbox.navigation.base.geometry.AngleUnit
import com.mapbox.navigation.base.internal.utils.safeCompareTo
import com.mapbox.navigation.base.physics.Speed
import com.mapbox.navigation.base.speed.model.SpeedUnit

/**
 * Represents raw GNSS location data, which includes various metrics such as altitude, bearing,
 * speed, latitude, and longitude.
 *
 * @param altitude The altitude of the location.
 * @param bearing The bearing of the location.
 * @param speed The speed at the location.
 * @param latitude The latitude of the location.
 * @param longitude The longitude of the location.
 * @param bearingAccuracy The accuracy of the bearing.
 * @param speedAccuracy The accuracy of the speed.
 * @param horizontalAccuracyMeters The horizontal accuracy of the location in meters.
 * @param verticalAccuracyMeters The vertical accuracy of the location in meters.
 */
@ExperimentalPreviewMapboxNavigationAPI
class RawGnssLocation(
    val altitude: Float,
    val bearing: Angle,
    val speed: Speed,
    val latitude: Float,
    val longitude: Float,
    val bearingAccuracy: Angle,
    val speedAccuracy: Speed,
    val horizontalAccuracyMeters: Float,
    val verticalAccuracyMeters: Float,
) {

    @JvmSynthetic
    internal fun mapToNative(): com.mapbox.navigator.RawGnssLocation {
        return com.mapbox.navigator.RawGnssLocation(
            altitude,
            bearing.toFloat(AngleUnit.DEGREES),
            speed.toFloat(SpeedUnit.METERS_PER_SECOND),
            latitude,
            longitude,
            bearingAccuracy.toFloat(AngleUnit.DEGREES),
            speedAccuracy.toFloat(SpeedUnit.METERS_PER_SECOND),
            horizontalAccuracyMeters,
            verticalAccuracyMeters,
        )
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RawGnssLocation

        if (!altitude.safeCompareTo(other.altitude)) return false
        if (bearing != other.bearing) return false
        if (speed != other.speed) return false
        if (!latitude.safeCompareTo(other.latitude)) return false
        if (!longitude.safeCompareTo(other.longitude)) return false
        if (bearingAccuracy != other.bearingAccuracy) return false
        if (speedAccuracy != other.speedAccuracy) return false
        if (!horizontalAccuracyMeters.safeCompareTo(other.horizontalAccuracyMeters)) return false
        return verticalAccuracyMeters.safeCompareTo(other.verticalAccuracyMeters)
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = altitude.hashCode()
        result = 31 * result + bearing.hashCode()
        result = 31 * result + speed.hashCode()
        result = 31 * result + latitude.hashCode()
        result = 31 * result + longitude.hashCode()
        result = 31 * result + bearingAccuracy.hashCode()
        result = 31 * result + speedAccuracy.hashCode()
        result = 31 * result + horizontalAccuracyMeters.hashCode()
        result = 31 * result + verticalAccuracyMeters.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "RawGnssLocation(" +
            "altitude=$altitude, " +
            "bearing=$bearing, " +
            "speed=$speed, " +
            "latitude=$latitude, " +
            "longitude=$longitude, " +
            "bearingAccuracy=$bearingAccuracy, " +
            "speedAccuracy=$speedAccuracy, " +
            "horizontalAccuracyMeters=$horizontalAccuracyMeters, " +
            "verticalAccuracyMeters=$verticalAccuracyMeters" +
            ")"
    }
}
