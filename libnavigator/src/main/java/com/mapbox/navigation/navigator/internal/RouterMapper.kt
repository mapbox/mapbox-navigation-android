package com.mapbox.navigation.navigator.internal

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterOrigin.Offboard
import com.mapbox.navigation.base.route.RouterOrigin.Onboard
import com.mapbox.navigator.RouterOrigin
import com.mapbox.navigator.RoutingMode

internal const val HTTP_SUCCESS_CODE = "Ok"

fun RouterOrigin.mapToSdkRouteOrigin(): com.mapbox.navigation.base.route.RouterOrigin {
    return when (this) {
        RouterOrigin.ONLINE -> Offboard
        RouterOrigin.ONBOARD -> Onboard
        RouterOrigin.CUSTOM -> com.mapbox.navigation.base.route.RouterOrigin.Custom()
    }
}

fun com.mapbox.navigation.base.route.RouterOrigin.mapToNativeRouteOrigin(): RouterOrigin =
    when (this) {
        Offboard -> RouterOrigin.ONLINE
        Onboard -> RouterOrigin.ONBOARD
        is com.mapbox.navigation.base.route.RouterOrigin.Custom -> RouterOrigin.CUSTOM
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

/**
 * Map list of [NavigationRoute] to [DirectionsResponse]
 *
 * Until [mapbox-navigation-native#5142](https://github.com/mapbox/mapbox-navigation-native/issues/5142) is resolved,
 * we'll keep providing injected routes into the first response.
 *
 * @throws IllegalStateException if list is empty
 */
internal fun List<NavigationRoute>.mapToDirectionsResponse(): DirectionsResponse {
    val primaryRoute = this.firstOrNull()
        ?: throw IllegalStateException("List of NavigationRoute mustn't be empty")
    return primaryRoute.directionsResponse.toBuilder()
        .routes(this.map { it.directionsRoute })
        .build()
}
