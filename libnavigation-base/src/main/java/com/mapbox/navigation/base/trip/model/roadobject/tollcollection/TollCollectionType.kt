package com.mapbox.navigation.base.trip.model.roadobject.tollcollection

import androidx.annotation.IntDef

/**
 * Utility that lists available toll collection point types.
 */
object TollCollectionType {
    /**
     * Describes an unknown toll collection point.
     *
     * This means that the type is either erroneous,
     * or there's a new type available server-side which will be exposed in the SDK in the future.
     */
    const val UNKNOWN = -1

    /**
     * Describes a payment booth.
     */
    const val TOLL_BOOTH = 1

    /**
     * Describes an overhead electronic gantry.
     */
    const val TOLL_GANTRY = 2

    /**
     * Toll collection type.
     */
    @Retention(AnnotationRetention.BINARY)
    @IntDef(
        UNKNOWN,
        TOLL_BOOTH,
        TOLL_GANTRY,
    )
    annotation class Type
}
