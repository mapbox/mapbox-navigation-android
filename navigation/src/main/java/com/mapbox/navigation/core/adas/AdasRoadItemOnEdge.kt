package com.mapbox.navigation.core.adas

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Road item on the edge.
 *
 * @param shapeIndex position on edge.
 *  Integer part is an index of edge segment and fraction
 *  is a position on the segment: 0 - left point, 1 - right point,
 *  0.5 - in the middle between the segment points.
 *  Ex.: 3.5 means the middle of the 3rd segment on the Edge shape, shape has more then 4 points
 * o---------------o-------------------------o-----------o -- edge
 *  segment 0         segment 1               segment2
 * @param percentAlong @param percentAlong Position along edge shape [0-1]
 * @param roadItem the road item.
 */
@ExperimentalPreviewMapboxNavigationAPI
class AdasRoadItemOnEdge private constructor(
    val shapeIndex: Float,
    val percentAlong: Double,
    val roadItem: AdasRoadItem,
) {

    internal companion object {

        @JvmSynthetic
        fun createFromNativeObject(
            nativeObj: com.mapbox.navigator.RoadItemOnEdge,
        ): AdasRoadItemOnEdge {
            return AdasRoadItemOnEdge(
                nativeObj.shapeIndex,
                nativeObj.percentAlong,
                AdasRoadItem.createFromNativeObject(nativeObj.roadItem),
            )
        }
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AdasRoadItemOnEdge

        if (shapeIndex != other.shapeIndex) return false
        if (percentAlong != other.percentAlong) return false
        if (roadItem != other.roadItem) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = shapeIndex.hashCode()
        result = 31 * result + percentAlong.hashCode()
        result = 31 * result + roadItem.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "AdasRoadItemOnEdge(" +
            "shapeIndex=$shapeIndex, " +
            "percentAlong=$percentAlong, " +
            "roadItem=$roadItem" +
            ")"
    }
}
