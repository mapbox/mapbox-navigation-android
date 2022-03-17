package com.mapbox.navigation.base.internal.route

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.LegAnnotation
import com.mapbox.navigation.base.internal.SDKRouteParser
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.toNavigationRoute
import com.mapbox.navigator.Navigator
import com.mapbox.navigator.RouteInterface

/**
 * Internal handle for the route's native peer.
 */
fun NavigationRoute.nativeRoute(): RouteInterface = this.nativeRoute

/**
 * Updates route's annotations in place while keeping the Native peer as is.
 * The peer should later be updated through [Navigator.updateAnnotations].
 */
fun NavigationRoute.updateLegAnnotations(
    initialLegIndex: Int,
    legAnnotations: List<LegAnnotation?>?
): NavigationRoute {
    val updateLegs = directionsRoute.legs()?.mapIndexed { index, routeLeg ->
        if (index < initialLegIndex) {
            routeLeg
        } else {
            routeLeg.toBuilder().annotation(
                legAnnotations?.getOrNull(index)
            ).build()
        }
    }
    val refreshedRoute = directionsRoute.toBuilder()
        .legs(updateLegs)
        .build()
    val refreshedRoutes = directionsResponse.routes().toMutableList().apply {
        removeAt(routeIndex)
        add(routeIndex, refreshedRoute)
    }
    val refreshedResponse = directionsResponse.toBuilder()
        .routes(refreshedRoutes)
        .build()
    return copy(directionsResponse = refreshedResponse)
}

/**
 * Internal API used for testing purposes. Needed to avoid calling native parser from unit tests.
 */
fun createNavigationRoute(
    directionsRoute: DirectionsRoute,
    sdkRouteParser: SDKRouteParser
): NavigationRoute = directionsRoute.toNavigationRoute(sdkRouteParser)
