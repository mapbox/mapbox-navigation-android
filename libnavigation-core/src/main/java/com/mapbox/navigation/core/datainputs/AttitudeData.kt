package com.mapbox.navigation.core.datainputs

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.geometry.Angle
import com.mapbox.navigation.base.geometry.AngleUnit

/**
 * Represents attitude data including pitch, yaw, and roll angles.
 *
 * @param pitch The pitch angle.
 * @param yaw The yaw angle.
 * @param roll The roll angle.
 */
@ExperimentalPreviewMapboxNavigationAPI
class AttitudeData(
    val pitch: Angle,
    val yaw: Angle,
    val roll: Angle,
) {

    @JvmSynthetic
    internal fun mapToNative(): com.mapbox.navigator.AttitudeData {
        return com.mapbox.navigator.AttitudeData(
            pitch.toFloat(AngleUnit.RADIANS),
            yaw.toFloat(AngleUnit.RADIANS),
            roll.toFloat(AngleUnit.RADIANS),
        )
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AttitudeData

        if (pitch != other.pitch) return false
        if (yaw != other.yaw) return false
        return roll == other.roll
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = pitch.hashCode()
        result = 31 * result + yaw.hashCode()
        result = 31 * result + roll.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "AttitudeData(" +
            "pitch=$pitch, " +
            "yaw=$yaw, " +
            "roll=$roll" +
            ")"
    }
}
