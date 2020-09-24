package com.mapbox.navigation.base.trip.model.alert

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
    const val Unknown = -1

    /**
     * Describes a rest area.
     */
    const val RestArea = 1

    /**
     * Describes a service area.
     */
    const val ServiceArea = 2
}
