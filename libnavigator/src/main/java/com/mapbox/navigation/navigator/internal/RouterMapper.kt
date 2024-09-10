package com.mapbox.navigation.navigator.internal

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.navigator.RoutingMode

fun String.mapToRoutingMode(): RoutingMode {
    return when (this) {
        DirectionsCriteria.PROFILE_CYCLING -> RoutingMode.CYCLING
        DirectionsCriteria.PROFILE_WALKING -> RoutingMode.WALKING
        DirectionsCriteria.PROFILE_DRIVING -> RoutingMode.DRIVING
        DirectionsCriteria.PROFILE_DRIVING_TRAFFIC -> RoutingMode.DRIVING_TRAFFIC
        else -> throw IllegalArgumentException("Invalid routing profile: $this")
    }
}
