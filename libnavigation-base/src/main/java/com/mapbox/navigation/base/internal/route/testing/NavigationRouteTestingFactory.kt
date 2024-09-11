@file:OptIn(ExperimentalMapboxNavigationAPI::class)

package com.mapbox.navigation.base.internal.route.testing

import androidx.annotation.VisibleForTesting
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.internal.SDKRouteParser
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.ResponseOriginAPI

/**
 * Internal API used for testing purposes. Needed to avoid calling native parser from unit tests.
 */
@VisibleForTesting
fun createNavigationRouteForTest(
    directionsResponse: DirectionsResponse,
    routeOptions: RouteOptions,
    routeParser: SDKRouteParser,
    @com.mapbox.navigation.base.route.RouterOrigin
    routerOrigin: String,
    responseTimeElapsedSeconds: Long?,
    @ResponseOriginAPI responseOriginAPI: String,
): List<NavigationRoute> =
    NavigationRoute.create(
        directionsResponse,
        routeOptions,
        routeParser,
        routerOrigin,
        responseTimeElapsedSeconds,
        responseOriginAPI,
    )

@VisibleForTesting
fun createNavigationRouteForTest(
    directionsResponse: DirectionsResponse,
    routeOptions: RouteOptions,
    @com.mapbox.navigation.base.route.RouterOrigin
    routerOrigin: String,
) = NavigationRoute.create(
    directionsResponse,
    routeOptions,
    routerOrigin,
)

@VisibleForTesting
fun createNavigationRouteForTest(
    directionsResponseJson: String,
    routeRequestUrl: String,
    @com.mapbox.navigation.base.route.RouterOrigin
    routerOrigin: String,
) = NavigationRoute.create(
    directionsResponseJson,
    routeRequestUrl,
    routerOrigin,
)
