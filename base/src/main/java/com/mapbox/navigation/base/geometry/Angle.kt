package com.mapbox.navigation.base.geometry

import com.mapbox.navigation.base.geometry.Angle.Companion.degrees
import com.mapbox.navigation.base.geometry.Angle.Companion.radians
import com.mapbox.navigation.base.geometry.Angle.Companion.toAngle
import com.mapbox.navigation.base.internal.utils.safeCompareTo

/**
 * Represents an angle measurement.
 *
 * To create an object use either the extension function [toAngle],
 * or the extension properties [degrees], or [radians] available on all [Number] types.
 *
 * @property value The value of the angle measurement. Note that the value is not normalized.
 * For example, 450 degrees will not be converted to 90 and remain as 450 degrees.
 * @property unit The unit of the angle measurement.
 */
class Angle private constructor(val value: Double, val unit: AngleUnit) {

    /**
     * Converts the angle to the specified unit.
     *
     * @param targetUnit The unit to which the angle should be converted.
     * @return Angle object converted to the specified unit. Returns this object if units match.
     */
    fun convert(targetUnit: AngleUnit): Angle {
        if (targetUnit == unit) {
            return this
        }
        return Angle(toDouble(targetUnit), targetUnit)
    }

    /**
     * Returns the value of this angle as a [Double] number of the specified [targetUnit].
     *
     * @param targetUnit The unit to which the angle should be converted.
     * @return Value as a [Double] converted to the specified unit.
     */
    fun toDouble(targetUnit: AngleUnit): Double {
        if (targetUnit == unit) {
            return value
        }

        return when (targetUnit) {
            AngleUnit.DEGREES -> Math.toDegrees(value)
            AngleUnit.RADIANS -> Math.toRadians(value)
        }
    }

    /**
     * Returns the value of this angle as a [Float] number of the specified [targetUnit].
     *
     * @param targetUnit The unit to which the angle should be converted.
     * @return Value as a [Float] converted to the specified unit.
     */
    fun toFloat(targetUnit: AngleUnit): Float {
        return toDouble(targetUnit).toFloat()
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Angle

        if (!value.safeCompareTo(other.value)) return false
        return unit == other.unit
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = value.hashCode()
        result = 31 * result + unit.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "Angle(value=$value, unit=$unit)"
    }

    companion object {

        /**
         * Returns this [Number] as an [Angle] measurement in the specified [AngleUnit].
         *
         * The [Angle.value] will be stored in the original [Angle.unit] without conversion.
         * To obtain the value in a different unit, use [Angle.convert], [Angle.toFloat],
         * or [Angle.toDouble], providing the target [AngleUnit] for conversion.
         *
         * @param unit The unit of this value.
         * @return An Angle object representing the value in the specified unit.
         */
        @JvmStatic
        fun Number.toAngle(unit: AngleUnit): Angle = Angle(toDouble(), unit)

        /**
         * Returns this [Number] to an angle measurement in degrees.
         */
        @JvmStatic
        val Number.degrees: Angle
            get() = Angle(toDouble(), AngleUnit.DEGREES)

        /**
         * Returns this [Number] to an angle measurement in radians.
         */
        @JvmStatic
        val Number.radians: Angle
            get() = Angle(toDouble(), AngleUnit.RADIANS)
    }
}
