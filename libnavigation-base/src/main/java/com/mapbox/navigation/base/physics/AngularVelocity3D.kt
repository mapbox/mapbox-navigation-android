package com.mapbox.navigation.base.physics

import com.mapbox.navigation.base.internal.utils.safeCompareTo

/**
 * Represents a three-dimensional angular velocity vector.
 *
 * To create an object use [radiansPerSecond] or [degreesPerSecond] functions.
 *
 * @property x The angular velocity component around the x-axis.
 * @property y The angular velocity component around the y-axis.
 * @property z The angular velocity component around the z-axis.
 * @property unit The unit of angular velocity measurement.
 */
class AngularVelocity3D private constructor(
    val x: Double,
    val y: Double,
    val z: Double,
    val unit: AngularVelocityUnit,
) {

    /**
     * Converts the angular velocity components to the specified unit.
     *
     * @param targetUnit The unit to which the angular velocity components should be converted.
     * @return AngularVelocity3D object with components converted to the specified unit.
     * Returns this object if units match.
     */
    fun convert(targetUnit: AngularVelocityUnit): AngularVelocity3D {
        if (targetUnit == unit) {
            return this
        }

        return AngularVelocity3D(
            x = convert(x, unit, targetUnit),
            y = convert(y, unit, targetUnit),
            z = convert(z, unit, targetUnit),
            unit = targetUnit,
        )
    }

    private fun convert(
        velocity: Double,
        from: AngularVelocityUnit,
        to: AngularVelocityUnit,
    ): Double {
        return if (from == to) {
            velocity
        } else {
            when (to) {
                AngularVelocityUnit.RADIANS_PER_SECOND -> Math.toRadians(velocity)
                AngularVelocityUnit.DEGREES_PER_SECOND -> Math.toDegrees(velocity)
            }
        }
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AngularVelocity3D

        if (!x.safeCompareTo(other.x)) return false
        if (!y.safeCompareTo(other.y)) return false
        if (!z.safeCompareTo(other.z)) return false
        return unit == other.unit
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = x.hashCode()
        result = 31 * result + y.hashCode()
        result = 31 * result + z.hashCode()
        result = 31 * result + unit.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "AngularVelocity3D(x=$x, y=$y, z=$z, unit=$unit)"
    }

    companion object {

        /**
         * Creates a three-dimensional angular velocity vector whose values are in
         * radians per second.
         *
         * @param x The angular velocity component around the x-axis.
         * @param y The angular velocity component around the y-axis.
         * @param z The angular velocity component around the z-axis.
         * @return An AngularVelocity3D object.
         */
        @JvmStatic
        fun radiansPerSecond(x: Double, y: Double, z: Double): AngularVelocity3D {
            return AngularVelocity3D(x, y, z, AngularVelocityUnit.RADIANS_PER_SECOND)
        }

        /**
         * Creates a three-dimensional angular velocity vector whose values are in
         * degrees per second.
         *
         * @param x The angular velocity component around the x-axis.
         * @param y The angular velocity component around the y-axis.
         * @param z The angular velocity component around the z-axis.
         * @return An AngularVelocity3D object.
         */
        @JvmStatic
        fun degreesPerSecond(x: Double, y: Double, z: Double): AngularVelocity3D {
            return AngularVelocity3D(x, y, z, AngularVelocityUnit.DEGREES_PER_SECOND)
        }
    }
}
