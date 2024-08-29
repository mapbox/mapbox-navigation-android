package com.mapbox.navigation.base.trip.model.roadobject.location

import androidx.annotation.IntDef

/**
 * OpenLROrientation describes the relationship between the road object and the direction of a
 * referenced line. The road object may be directed in the same direction as the line, against
 * that direction, both directions, or the direction of the road object might be unknown.
 */
object OpenLROrientation {
    /**
     * Type of the [NO_ORIENTATION_OR_UNKNOWN].
     */
    const val NO_ORIENTATION_OR_UNKNOWN = 0

    /**
     * Type of the [WITH_LINE_DIRECTION].
     */
    const val WITH_LINE_DIRECTION = 1

    /**
     * Type of the [AGAINST_LINE_DIRECTION].
     */
    const val AGAINST_LINE_DIRECTION = 2

    /**
     * Type of the [BOTH].
     */
    const val BOTH = 3

    /**
     * Retention policy for the OpenLROrientation
     */
    @Retention(AnnotationRetention.BINARY)
    @IntDef(
        NO_ORIENTATION_OR_UNKNOWN,
        WITH_LINE_DIRECTION,
        AGAINST_LINE_DIRECTION,
        BOTH,
    )
    annotation class Type
}
