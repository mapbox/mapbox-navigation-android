package com.mapbox.navigation.base.trip.model.roadobject.reststop

import androidx.annotation.IntDef

/**
 * Utility that lists available rest stops point types.
 */
object RestStopType {
    /**
     * Describes an unknown rest stop.
     *
     * This means that the type is either erroneous,
     * or there's a new type available server-side which will be exposed in the SDK in the future.
     */
    const val UNKNOWN = -1

    /**
     * Describes a rest area.
     */
    const val REST_AREA = 1

    /**
     * Describes a service area.
     */
    const val SERVICE_AREA = 2

    /**
     * Rest stop type.
     */
    @Retention(AnnotationRetention.BINARY)
    @IntDef(
        UNKNOWN,
        REST_AREA,
        SERVICE_AREA,
    )
    annotation class Type
}
