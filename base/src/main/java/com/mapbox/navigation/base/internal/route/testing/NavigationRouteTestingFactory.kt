@file:OptIn(ExperimentalMapboxNavigationAPI::class)

package com.mapbox.navigation.base.internal.route.testing

import androidx.annotation.VisibleForTesting
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.internal.SDKRouteParser
import com.mapbox.navigation.base.internal.route.parsing.DirectionsResponseToParse
import com.mapbox.navigation.base.internal.route.parsing.setupParsing
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.ResponseOriginAPI
import com.mapbox.navigation.utils.internal.LoggerFrontend
import com.mapbox.navigation.utils.internal.LoggerProvider
import com.mapbox.navigation.utils.internal.Time
import kotlinx.coroutines.runBlocking

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
    logger: LoggerFrontend = LoggerProvider.getLoggerFrontend(),
): List<NavigationRoute> =
    setupParsing(
        nativeRoute = false,
        nnParser = routeParser,
        time = responseTimeElapsedSeconds?.let { StaticTime(it) } ?: Time.SystemImpl,
        loggerFrontend = logger,
    ).let {
        runBlocking {
            it.parseDirectionsResponse(
                DirectionsResponseToParse.from(
                    responseBody = directionsResponse.toJson().toDataRefJava(),
                    routeRequest = routeOptions.toUrl("***").toString(),
                    routerOrigin = routerOrigin,
                    responseOriginAPI = responseOriginAPI,
                ),
            ).getOrThrow().routes
        }
    }

@VisibleForTesting
fun createNavigationRouteForTest(
    directionsResponse: DirectionsResponse,
    routeOptions: RouteOptions,
    @com.mapbox.navigation.base.route.RouterOrigin
    routerOrigin: String,
): List<NavigationRoute> = setupParsing(nativeRoute = false).let {
    runBlocking {
        it.parseDirectionsResponse(
            DirectionsResponseToParse.from(
                responseBody = directionsResponse.toJson().toDataRefJava(),
                routeRequest = routeOptions.toUrl("***").toString(),
                routerOrigin = routerOrigin,
            ),
        ).getOrThrow().routes
    }
}

@VisibleForTesting
fun createNavigationRouteForTest(
    directionsResponseJson: String,
    routeRequestUrl: String,
    @com.mapbox.navigation.base.route.RouterOrigin
    routerOrigin: String,
) = setupParsing(nativeRoute = false).let {
    runBlocking {
        it.parseDirectionsResponse(
            DirectionsResponseToParse.from(
                responseBody = directionsResponseJson.toDataRefJava(),
                routeRequest = routeRequestUrl,
                routerOrigin = routerOrigin,
            ),
        ).getOrThrow().routes
    }
}

private class StaticTime(private val seconds: Long) : Time {
    override fun nanoTime(): Long {
        return seconds * 1_000_000_000
    }

    override fun millis(): Long {
        return seconds * 1_000
    }

    override fun seconds(): Long {
        return seconds
    }
}
