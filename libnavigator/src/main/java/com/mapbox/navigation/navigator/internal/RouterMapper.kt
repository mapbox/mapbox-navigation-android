@file:JvmName("RouterMapper")

package com.mapbox.navigation.navigator.internal

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.navigation.base.route.NavigationRoute
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

/**
 * Map list of [NavigationRoute] to [DirectionsResponse]
 *
 * Until https://github.com/mapbox/mapbox-navigation-native/issues/5554 is resolved,
 * we'll keep providing injected routes into the first response.
 *
 * @throws IllegalStateException if list is empty
 */
fun List<NavigationRoute>.mapToDirectionsResponse(): DirectionsResponse {
    val primaryRoute = this.firstOrNull()
        ?: throw IllegalStateException("List of NavigationRoute mustn't be empty")
    return primaryRoute.directionsResponse.toBuilder()
        .routes(this.map { it.directionsRoute })
        .build()
}
