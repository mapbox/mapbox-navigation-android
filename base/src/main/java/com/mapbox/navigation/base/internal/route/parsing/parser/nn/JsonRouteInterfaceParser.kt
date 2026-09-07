@file:OptIn(ExperimentalMapboxNavigationAPI::class)

package com.mapbox.navigation.base.internal.route.parsing.parser.nn

import com.mapbox.bindgen.DataRef
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.internal.performance.PerformanceTracker
import com.mapbox.navigation.base.internal.route.parsing.ResponseToParse
import com.mapbox.navigation.base.internal.route.parsing.models.directions.DirectionsResponseParsingResult
import com.mapbox.navigation.base.internal.route.parsing.models.directions.DirectionsRoutesParser
import com.mapbox.navigation.base.internal.route.parsing.models.nn.ContinuousAlternativesParsingSuccessfulResult
import com.mapbox.navigation.base.internal.route.parsing.models.nn.RouteInterfacesParser
import com.mapbox.navigation.base.internal.utils.AlternativesInfo
import com.mapbox.navigation.base.internal.utils.AlternativesParsingResult
import com.mapbox.navigation.base.internal.utils.RouteParsingQueue
import com.mapbox.navigation.base.internal.utils.RouteResponseInfo
import com.mapbox.navigation.base.internal.utils.mapToSDKResponseOriginAPI
import com.mapbox.navigation.base.internal.utils.mapToSdkRouteOrigin
import com.mapbox.navigation.base.internal.utils.refreshTtl
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.utils.internal.Time
import com.mapbox.navigation.utils.internal.logE
import com.mapbox.navigation.utils.internal.logI
import com.mapbox.navigator.RouteInterface
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

private const val LOG_CATEGORY = "JsonRouteInterfaceParser"

/**
 * Parses [RouteInterface]s into [NavigationRoute]s, reusing an already parsed [NavigationRoute]
 * for a given route id instead of parsing it again.
 */
internal class JsonRouteInterfaceParser(
    private val existingParsedRoutesLookup: (id: String) -> NavigationRoute?,
    private val parsingDispatcher: CoroutineDispatcher,
    private val time: Time,
    private val parser: DirectionsRoutesParser,
    private val parsingQueue: RouteParsingQueue,
) : RouteInterfacesParser {
    override suspend fun parserContinuousAlternatives(
        routes: List<RouteInterface>,
    ): AlternativesParsingResult<Result<ContinuousAlternativesParsingSuccessfulResult>> {
        val responseTimeElapsedSeconds = time.seconds()

        val routesToParse = routes.map { route ->
            val cachedRoute = existingParsedRoutesLookup(route.routeId)
            RouteToParse(
                route = route,
                cachedRoute = cachedRoute,
                // toJson() produces new data every call, so it's computed once and reused.
                json = if (cachedRoute == null) {
                    PerformanceTracker.trackPerformanceSync("RouteInterface#toJson") {
                        route.toJson()
                    }
                } else {
                    null
                },
            )
        }

        return parsingQueue.parseAlternatives(
            AlternativesInfo(
                RouteResponseInfo.fromRoutes(routesToParse.mapNotNull { it.json?.buffer }),
            ),
        ) {
            withContext(parsingDispatcher) {
                Result.runCatching {
                    parse(routesToParse, responseTimeElapsedSeconds)
                }.onFailure {
                    logE { "Alternative route parsing failed: ${it.message}" }
                }.map {
                    ContinuousAlternativesParsingSuccessfulResult(it)
                }
            }
        }
    }

    private fun parse(
        routesToParse: List<RouteToParse>,
        responseTimeElapsedSeconds: Long,
    ): List<NavigationRoute> = routesToParse.map { (route, cachedRoute, json) ->
        cachedRoute ?: run {
            logI(LOG_CATEGORY) {
                "parsing ${route.routeId}"
            }
            route.toNavigationRoute(
                responseTimeElapsedSeconds,
                parser.parse(
                    ResponseToParse(
                        // json is always set when cachedRoute is null, see routesToParse above
                        requireNotNull(json),
                        route.requestUri,
                        routerOrigin = route.routerOrigin.mapToSdkRouteOrigin(),
                        responseOriginAPI = route.mapboxAPI.mapToSDKResponseOriginAPI(),
                        routeIndexOverride = route.routeIndex,
                    ),
                ).getOrThrow(),
            )
        }
    }
}

private data class RouteToParse(
    val route: RouteInterface,
    val cachedRoute: NavigationRoute?,
    val json: DataRef?,
)

@OptIn(ExperimentalMapboxNavigationAPI::class)
private fun RouteInterface.toNavigationRoute(
    responseTimeElapsedSeconds: Long,
    parsedRoute: DirectionsResponseParsingResult,
): NavigationRoute {
    // toJson() always yields a single route, so it's at index 0 regardless of this route's
    // true index (restored via ResponseToParse.routeIndexOverride).
    val refreshTtl =
        parsedRoute.routesParsingResult.getOrNull(0)?.data?.route?.refreshTtl()
    val routeOptions = parsedRoute.routeOptions
    val data = parsedRoute.routesParsingResult[0].data
    return NavigationRoute(
        routeOptions = routeOptions,
        // TODO: test that route options are the same as with direct parsing
        directionsRoute = data.route,
        waypoints = data.routesWaypoint,
        nativeRoute = this,
        expirationTimeElapsedSeconds = refreshTtl?.plus(responseTimeElapsedSeconds),
        // TODO: adopt native parsing NAVAND-1732 to prevent response origin API being lost here
        // when existing route is reparsed
        responseOriginAPI = data.responseOriginAPI,
        // TODO: NAVAND-6774, move overriden traffic to native route
        overriddenTraffic = null,
        operations = parsedRoute.routesParsingResult[0].operations,
        directionsRouteContext = directionsRouteContext,
    )
}
