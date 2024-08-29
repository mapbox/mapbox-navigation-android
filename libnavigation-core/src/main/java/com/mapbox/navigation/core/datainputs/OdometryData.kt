package com.mapbox.navigation.core.datainputs

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.geometry.Angle
import com.mapbox.navigation.base.geometry.AngleUnit
import com.mapbox.navigation.base.internal.utils.safeCompareTo

/**
 * Represents odometry data which includes the position and orientation of a vehicle.
 *
 * @param x The x-coordinate of the position in the vehicle's coordinate system, in meters.
 * @param y The y-coordinate of the position in the vehicle's coordinate system, in meters.
 * @param z The z-coordinate of the position in the vehicle's coordinate system, in meters.
 * @param yawAngle The bearing of the car in the vehicle’s local coordinate system.
 * @param monotonicTimestampNanoseconds Timestamp which should be in sync with timestamps of
 * locations from location provider.
 */
@ExperimentalPreviewMapboxNavigationAPI
class OdometryData(
    val x: Float,
    val y: Float,
    val z: Float,
    val yawAngle: Angle,
    val monotonicTimestampNanoseconds: Long,
) {

    @JvmSynthetic
    internal fun mapToNative(): com.mapbox.navigator.OdometryData {
        return com.mapbox.navigator.OdometryData(
            x,
            y,
            z,
            yawAngle.toFloat(AngleUnit.DEGREES),
            monotonicTimestampNanoseconds,
        )
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OdometryData

        if (!x.safeCompareTo(other.x)) return false
        if (!y.safeCompareTo(other.y)) return false
        if (!z.safeCompareTo(other.z)) return false
        if (yawAngle != other.yawAngle) return false
        return monotonicTimestampNanoseconds == other.monotonicTimestampNanoseconds
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = x.hashCode()
        result = 31 * result + y.hashCode()
        result = 31 * result + z.hashCode()
        result = 31 * result + yawAngle.hashCode()
        result = 31 * result + monotonicTimestampNanoseconds.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "OdometryData(" +
            "x=$x, " +
            "y=$y, " +
            "z=$z, " +
            "yawAngle=$yawAngle, " +
            "monotonicTimestampNanoseconds=$monotonicTimestampNanoseconds" +
            ")"
    }
}
