package com.mapbox.navigation.base.model

import androidx.annotation.IntDef
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Type of vehicle for which the speed limit is included.
 */
@ExperimentalPreviewMapboxNavigationAPI
object VehicleType {

    /**
     * Car vehicle type
     */
    const val CAR = 0

    /**
     * Truck vehicle type
     */
    const val TRUCK = 1

    /**
     * Bus vehicle type
     */
    const val BUS = 2

    /**
     * Trailer vehicle type, it can be any vehicle with a trailer, including a car with a trailer.
     */
    const val TRAILER = 3

    /**
     * Motorcycle vehicle type
     */
    const val MOTORCYCLE = 4

    /**
     * Retention policy for the [VehicleType]
     */
    @Retention(AnnotationRetention.BINARY)
    @IntDef(
        CAR,
        TRUCK,
        BUS,
        TRAILER,
        MOTORCYCLE,
    )
    annotation class Type
}
