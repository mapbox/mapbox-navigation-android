package com.mapbox.navigation.core.adasis

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Position on the edge.
 *
 * @param shapeIndex Position on edge is represented as a shape index.
 * Integer part is an index of edge segment and fraction is a position on the segment:
 * 0 - left point, 1 - right point, 0.5 - in the middle between the segment points.
 * Ex.: 3.5 means the middle the the 3rd segment on the Edge shape, shape has more then 4 points
 * @param value Floating point value, e.g. curvature in 1/m or slope as {elevation diff}/{horizontal length}
 */
@ExperimentalPreviewMapboxNavigationAPI
class ValueOnEdge private constructor(
    val shapeIndex: Float,
    val value: Double,
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ValueOnEdge

        if (shapeIndex != other.shapeIndex) return false
        if (value != other.value) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = shapeIndex.hashCode()
        result = 31 * result + value.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "ValueOnEdge(shapeIndex=$shapeIndex, value=$value)"
    }

    internal companion object {

        @JvmSynthetic
        fun createFromNativeObject(nativeObj: com.mapbox.navigator.ValueOnEdge) =
            ValueOnEdge(
                shapeIndex = nativeObj.shapeIndex,
                value = nativeObj.value
            )
    }
}
