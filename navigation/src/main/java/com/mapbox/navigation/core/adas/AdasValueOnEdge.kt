package com.mapbox.navigation.core.adas

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.internal.utils.safeCompareTo

/**
 * Position on the edge.
 *
 * @param shapeIndex Position on edge is represented as a shape index.
 * Integer part is an index of edge segment and fraction is a position on the segment:
 * 0 - left point, 1 - right point, 0.5 - in the middle between the segment points.
 * Ex.: 3.5 means the middle of the 3rd segment on the Edge shape, shape has more than 4 points
 * @param percentAlong Position along edge shape [0-1]
 * @param value Floating point value, e.g. curvature in 1/m or slope in degrees
 */
@ExperimentalPreviewMapboxNavigationAPI
class AdasValueOnEdge private constructor(
    val shapeIndex: Float,
    val percentAlong: Double,
    val value: Double,
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AdasValueOnEdge

        if (!shapeIndex.safeCompareTo(other.shapeIndex)) return false
        if (!percentAlong.safeCompareTo(other.percentAlong)) return false
        return value.safeCompareTo(other.value)
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = shapeIndex.hashCode()
        result = 31 * result + percentAlong.hashCode()
        result = 31 * result + value.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "ValueOnEdge(" +
            "shapeIndex=$shapeIndex, " +
            "percentAlong=$percentAlong, " +
            "value=$value" +
            ")"
    }

    internal companion object {

        @JvmSynthetic
        fun createFromNativeObject(nativeObj: com.mapbox.navigator.ValueOnEdge) =
            AdasValueOnEdge(
                shapeIndex = nativeObj.shapeIndex,
                percentAlong = nativeObj.percentAlong,
                value = nativeObj.value,
            )
    }
}
