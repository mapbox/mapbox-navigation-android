package com.mapbox.navigation.navigator.internal

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.navigation.base.route.RouterOrigin.Offboard
import com.mapbox.navigation.base.route.RouterOrigin.Onboard
import com.mapbox.navigator.RouterOrigin
import com.mapbox.navigator.RoutingMode

fun RouterOrigin.mapToSdkRouteOrigin(): com.mapbox.navigation.base.route.RouterOrigin {
    return when (this) {
        RouterOrigin.ONLINE -> Offboard
        RouterOrigin.ONBOARD -> Onboard
        // not actual till integrate sdk Router to NN
        RouterOrigin.CUSTOM -> com.mapbox.navigation.base.route.RouterOrigin.Custom()
    }
}

fun String.mapToRoutingMode(): RoutingMode {
    return when (this) {
        DirectionsCriteria.PROFILE_CYCLING -> RoutingMode.CYCLING
        DirectionsCriteria.PROFILE_WALKING -> RoutingMode.WALKING
        DirectionsCriteria.PROFILE_DRIVING -> RoutingMode.DRIVING
        DirectionsCriteria.PROFILE_DRIVING_TRAFFIC -> RoutingMode.DRIVING_TRAFFIC
        else -> throw IllegalArgumentException("Invalid routing profile: $this")
    }
}
