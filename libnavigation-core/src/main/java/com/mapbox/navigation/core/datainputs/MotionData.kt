package com.mapbox.navigation.core.datainputs

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.geometry.Angle
import com.mapbox.navigation.base.geometry.AngleUnit
import com.mapbox.navigation.base.geometry.Point3D
import com.mapbox.navigation.base.internal.mapToNative
import com.mapbox.navigation.base.internal.mapToNativePoint3DRadiansPerSecond
import com.mapbox.navigation.base.physics.AngularVelocity3D

/**
 * Represents motion data including attitude, rotation rate, accelerations, magnetic field,
 * heading, and timestamp.
 *
 * @param attitude The attitude data including pitch, yaw, and roll angles.
 * @param rotationRate The angular speed around axes in radians per second.
 * @param gravityAcceleration The gravity acceleration vector expressed in the
 * device's reference frame (measured in g units).
 * @param userAcceleration The acceleration that the user is giving to the device
 * (measured in g units).
 * @param magneticField The magnetic field vector with respect to the device
 * (measured in microteslas).
 * @param heading The heading angle relative to the current reference frame.
 * A heading value of 0 degrees (or radians) indicates that the attitude of the device
 * matches the current reference frame.
 * @param monotonicTimestampNanoseconds Timestamp which should be in sync with timestamps of
 * locations from location provider.
 */
@ExperimentalPreviewMapboxNavigationAPI
class MotionData(
    val attitude: AttitudeData,
    val rotationRate: AngularVelocity3D,
    val gravityAcceleration: Point3D,
    val userAcceleration: Point3D,
    val magneticField: Point3D,
    val heading: Angle,
    val monotonicTimestampNanoseconds: Long,
) {

    @JvmSynthetic
    internal fun mapToNative(): com.mapbox.navigator.MotionData {
        return com.mapbox.navigator.MotionData(
            attitude.mapToNative(),
            rotationRate.mapToNativePoint3DRadiansPerSecond(),
            gravityAcceleration.mapToNative(),
            userAcceleration.mapToNative(),
            magneticField.mapToNative(),
            heading.convert(AngleUnit.DEGREES).value.toFloat(),
            monotonicTimestampNanoseconds,
        )
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MotionData

        if (attitude != other.attitude) return false
        if (rotationRate != other.rotationRate) return false
        if (gravityAcceleration != other.gravityAcceleration) return false
        if (userAcceleration != other.userAcceleration) return false
        if (magneticField != other.magneticField) return false
        if (heading != other.heading) return false
        return monotonicTimestampNanoseconds == other.monotonicTimestampNanoseconds
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = attitude.hashCode()
        result = 31 * result + rotationRate.hashCode()
        result = 31 * result + gravityAcceleration.hashCode()
        result = 31 * result + userAcceleration.hashCode()
        result = 31 * result + magneticField.hashCode()
        result = 31 * result + heading.hashCode()
        result = 31 * result + monotonicTimestampNanoseconds.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "MotionData(" +
            "attitude=$attitude, " +
            "rotationRate=$rotationRate, " +
            "gravityAcceleration=$gravityAcceleration, " +
            "userAcceleration=$userAcceleration, " +
            "magneticField=$magneticField, " +
            "heading=$heading, " +
            "monotonicTimestampNanoseconds=$monotonicTimestampNanoseconds" +
            ")"
    }
}
