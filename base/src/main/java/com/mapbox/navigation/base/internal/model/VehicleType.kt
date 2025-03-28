package com.mapbox.navigation.base.internal.model

import androidx.annotation.RestrictTo
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.model.VehicleType

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@VehicleType.Type
fun com.mapbox.navigator.VehicleType.toPlatformVehicleType(): Int {
    return when (this) {
        com.mapbox.navigator.VehicleType.CAR -> VehicleType.CAR
        com.mapbox.navigator.VehicleType.TRUCK -> VehicleType.TRUCK
        com.mapbox.navigator.VehicleType.BUS -> VehicleType.BUS
        com.mapbox.navigator.VehicleType.TRAILER -> VehicleType.TRAILER
        com.mapbox.navigator.VehicleType.MOTORCYCLE -> VehicleType.MOTORCYCLE
    }
}
