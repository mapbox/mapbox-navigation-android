package com.mapbox.navigation.core.adas

import androidx.annotation.IntDef
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Location of the road item.
 */
@ExperimentalPreviewMapboxNavigationAPI
object AdasRoadItemLocation {

    /**
     * To the right.
     */
    const val RIGHT = 0

    /**
     * To the left.
     */
    const val LEFT = 1

    /**
     * Above.
     */
    const val ABOVE = 2

    /**
     * On the surface.
     */
    const val ON_SURFACE = 3

    /**
     * Above the lane.
     */
    const val ABOVE_LANE = 4

    /**
     * Retention policy for the [AdasRoadItemLocation.Location].
     */
    @Retention(AnnotationRetention.BINARY)
    @IntDef(
        RIGHT,
        LEFT,
        ABOVE,
        ON_SURFACE,
        ABOVE_LANE,
    )
    annotation class Location

    @JvmSynthetic
    @Location
    internal fun createFromNativeObject(
        nativeObj: com.mapbox.navigator.RoadItemLocation,
    ): Int {
        return when (nativeObj) {
            com.mapbox.navigator.RoadItemLocation.RIGHT -> RIGHT
            com.mapbox.navigator.RoadItemLocation.LEFT -> LEFT
            com.mapbox.navigator.RoadItemLocation.ABOVE -> ABOVE
            com.mapbox.navigator.RoadItemLocation.ON_SURFACE -> ON_SURFACE
            com.mapbox.navigator.RoadItemLocation.ABOVE_LANE -> ABOVE_LANE
        }
    }
}
