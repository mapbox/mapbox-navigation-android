package com.mapbox.navigation.core.adas

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Edge Adas Attributes
 *
 * @param speedLimit List of speed limits on the edge. Empty means no speed-limit data for the edge.
 * Multiple values will have different conditions.
 * @param slopes List of slope values with their positions on the edge.
 * Position is a shape index, where integer part in an index of geometry segment is
 * and fractional part is a position on the segment. Value is a slope in degrees
 * @param curvatures List of curvature values with their positions on the edge.
 * Position is a shape index, where integer part in an index of geometry segment is
 * and fractional part is a position on the segment
 */
@ExperimentalPreviewMapboxNavigationAPI
internal class AdasEdgeAttributes private constructor(
    val speedLimit: List<AdasSpeedLimitInfo>,
    val slopes: List<AdasValueOnEdge>,
    val curvatures: List<AdasValueOnEdge>,
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AdasEdgeAttributes

        if (speedLimit != other.speedLimit) return false
        if (slopes != other.slopes) return false
        return curvatures == other.curvatures
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = speedLimit.hashCode()
        result = 31 * result + slopes.hashCode()
        result = 31 * result + curvatures.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "EdgeAdasAttributes(speedLimit=$speedLimit, slopes=$slopes, curvatures=$curvatures)"
    }

    internal companion object {

        @JvmSynthetic
        fun createFromNativeObject(nativeObj: com.mapbox.navigator.EdgeAdasAttributes) =
            AdasEdgeAttributes(
                speedLimit = nativeObj.speedLimit.map {
                    AdasSpeedLimitInfo.createFromNativeObject(it)
                },
                slopes = nativeObj.slopes.map {
                    AdasValueOnEdge.createFromNativeObject(it)
                },
                curvatures = nativeObj.curvatures.map {
                    AdasValueOnEdge.createFromNativeObject(it)
                },
            )
    }
}
