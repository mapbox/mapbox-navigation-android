package com.mapbox.navigation.base.trip.model.roadobject.merge

import androidx.annotation.StringDef

/**
 * Holds available [MergingAreaInfo] types.
 */
object MergingAreaType {

    /**
     * Indicates that traffic is merged from the left.
     */
    const val FROM_LEFT = "from_left"

    /**
     * Indicates that traffic is merged from the right.
     */
    const val FROM_RIGHT = "from_right"

    /**
     * Indicates that traffic is merged from both sides.
     */
    const val FROM_BOTH_SIDES = "from_both_sides"

    /**
     * Retention policy for the MergingAreaType.
     */
    @Retention(AnnotationRetention.BINARY)
    @StringDef(
        FROM_LEFT,
        FROM_RIGHT,
        FROM_BOTH_SIDES,
    )
    annotation class Type
}
