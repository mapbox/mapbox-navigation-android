package com.mapbox.navigation.base.geometry

import com.mapbox.navigation.base.internal.utils.safeCompareTo

/**
 * Represents a 3-dimensional point with coordinates (x, y, z).
 *
 * @param x The x-coordinate of the point.
 * @param y The y-coordinate of the point.
 * @param z The z-coordinate of the point.
 */
class Point3D(
    val x: Double,
    val y: Double,
    val z: Double,
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Point3D

        if (!x.safeCompareTo(other.x)) return false
        if (!y.safeCompareTo(other.y)) return false
        return z.safeCompareTo(other.z)
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = x.hashCode()
        result = 31 * result + y.hashCode()
        result = 31 * result + z.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "Point3d(x=$x, y=$y, z=$z)"
    }
}
