package com.mapbox.navigation.core.adasis

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
class AdasisEdgeAttributes private constructor(
    val speedLimit: List<AdasisSpeedLimitInfo>,
    val slopes: List<AdasisValueOnEdge>,
    val curvatures: List<AdasisValueOnEdge>,
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AdasisEdgeAttributes

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
            AdasisEdgeAttributes(
                speedLimit = nativeObj.speedLimit.map {
                    AdasisSpeedLimitInfo.createFromNativeObject(it)
                },
                slopes = nativeObj.slopes.map {
                    AdasisValueOnEdge.createFromNativeObject(it)
                },
                curvatures = nativeObj.curvatures.map {
                    AdasisValueOnEdge.createFromNativeObject(it)
                },
            )
    }
}
