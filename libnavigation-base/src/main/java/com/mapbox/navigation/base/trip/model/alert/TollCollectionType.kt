package com.mapbox.navigation.base.trip.model.alert

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
    const val Unknown = -1

    /**
     * Describes a payment booth.
     */
    const val TollBooth = 1

    /**
     * Describes an overhead electronic gantry.
     */
    const val TollGantry = 2
}
