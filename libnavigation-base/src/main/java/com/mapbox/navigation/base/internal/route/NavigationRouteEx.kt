@file:JvmName("NavigationRouteEx")

package com.mapbox.navigation.base.internal.route

import androidx.annotation.VisibleForTesting
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.Incident
import com.mapbox.api.directions.v5.models.LegAnnotation
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.internal.SDKRouteParser
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.toNavigationRoute
import com.mapbox.navigation.base.utils.DecodeUtils.legsGeometryToPoints
import com.mapbox.navigator.Navigator
import com.mapbox.navigator.RouteInterface
import com.mapbox.navigator.RouterOrigin
import org.jetbrains.annotations.TestOnly

val NavigationRoute.routerOrigin: RouterOrigin get() = nativeRoute.routerOrigin

/**
 * Internal handle for the route's native peer.
 */
fun NavigationRoute.nativeRoute(): RouteInterface = this.nativeRoute

/**
 * Updates route's annotations and incidents in place while keeping the Native peer as is.
 * The peer should later be updated through [Navigator.refreshRoute].
 */
fun NavigationRoute.refreshRoute(
    initialLegIndex: Int,
    currentRouteGeometryIndex: Int?,
    legAnnotations: List<LegAnnotation?>?,
    incidents: List<List<Incident>?>?,
): NavigationRoute {
    val updateLegs = directionsRoute.legs()?.mapIndexed { index, routeLeg ->
        if (index < initialLegIndex) {
            routeLeg
        } else {
            val newAnnotation = legAnnotations?.getOrNull(index)
            val mergedAnnotation = if (index == initialLegIndex && currentRouteGeometryIndex != null) {
                val legGeometryIndexOffset = directionsRoute.legsGeometryToPoints()
                    .sumOfUntil(index) { it.size }
                AnnotationsRefresher.getRefreshedAnnotations(
                    routeLeg.annotation(),
                    newAnnotation,
                    currentRouteGeometryIndex - legGeometryIndexOffset
                )
            } else {
                newAnnotation
            }
            routeLeg.toBuilder()
                .incidents(incidents?.getOrNull(index))
                .annotation(mergedAnnotation)
                .build()
        }
    }
    return updateDirectionsRouteOnly {
        toBuilder().legs(updateLegs).build()
    }
}

/**
 * Updates only java representation of route.
 * The native route should later be updated through [Navigator.refreshRoute].
 */
fun NavigationRoute.updateDirectionsRouteOnly(
    block: DirectionsRoute.() -> DirectionsRoute
): NavigationRoute {
    val refreshedRoute = directionsRoute.block()
    val refreshedRoutes = directionsResponse.routes().toMutableList()
    refreshedRoutes[routeIndex] = refreshedRoute
    val refreshedResponse = directionsResponse.toBuilder()
        .routes(refreshedRoutes)
        .build()
    return copy(directionsResponse = refreshedResponse)
}

/**
 * Internal API used for testing purposes. Needed to avoid calling native parser from unit tests.
 */
@TestOnly
fun createNavigationRoute(
    directionsRoute: DirectionsRoute,
    sdkRouteParser: SDKRouteParser,
): NavigationRoute =
    directionsRoute
        .toNavigationRoute(sdkRouteParser, com.mapbox.navigation.base.route.RouterOrigin.Custom())

/**
 * Internal API used for testing purposes. Needed to avoid calling native parser from unit tests.
 */
@VisibleForTesting
fun createNavigationRoutes(
    directionsResponse: DirectionsResponse,
    routeOptions: RouteOptions,
    routeParser: SDKRouteParser,
    routerOrigin: com.mapbox.navigation.base.route.RouterOrigin,
): List<NavigationRoute> =
    NavigationRoute.create(directionsResponse, routeOptions, routeParser, routerOrigin)

/**
 * Internal API to create a new [NavigationRoute] from a native peer.
 */
fun RouteInterface.toNavigationRoute(): NavigationRoute {
    return this.toNavigationRoute()
}

private inline fun <T> List<T>.sumOfUntil(lastIndex: Int, selector: (T) -> Int): Int {
    var sum = 0
    for (index in 0 until lastIndex) {
        sum += selector(get(index))
    }
    return sum
}
