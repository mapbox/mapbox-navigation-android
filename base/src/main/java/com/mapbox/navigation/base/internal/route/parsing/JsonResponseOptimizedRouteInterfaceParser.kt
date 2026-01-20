@file:OptIn(ExperimentalMapboxNavigationAPI::class)

package com.mapbox.navigation.base.internal.route.parsing

import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.internal.route.parsing.models.DirectionsResponseParsingResult
import com.mapbox.navigation.base.internal.route.parsing.models.RouteModelsParser
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

private const val LOG_CATEGORY = "JsonResponseOptimizedRouteInterfaceParser"

/**
 * Optimized parser contains the following optimizations:
 * 1. Makes sure that every response is parsed only once in case routes in list are from different responses
 * 2. Makes sure that routes which already parsed aren't parsed again
 */
internal class JsonResponseOptimizedRouteInterfaceParser(
    private val existingParsedRoutesLookup: (id: String) -> NavigationRoute?,
    private val parsingDispatcher: CoroutineDispatcher,
    private val time: Time,
    private val parser: RouteModelsParser,
    private val parsingQueue: RouteParsingQueue,
) : RouteInterfacesParser {
    override suspend fun parserContinuousAlternatives(
        routes: List<RouteInterface>,
    ): AlternativesParsingResult<Result<ContinuousAlternativesParsingSuccessfulResult>> {
        val responseTimeElapsedSeconds = time.seconds()

        return parsingQueue.parseAlternatives(
            AlternativesInfo(
                RouteResponseInfo.Companion.fromResponses(
                    routes.map { it.responseJsonRef.buffer },
                ),
            ),
        ) {
            withContext(parsingDispatcher) {
                Result.runCatching {
                    parse(routes, responseTimeElapsedSeconds)
                }.onFailure {
                    logE { "Alternative route parsing failed: ${it.message}" }
                }.map {
                    ContinuousAlternativesParsingSuccessfulResult(it)
                }
            }
        }
    }

    private fun parse(
        routes: List<RouteInterface>,
        responseTimeElapsedSeconds: Long,
    ): List<NavigationRoute> = routes.groupBy { it.responseUuid }
        .map { (_, routes) ->
            val routesFromAssociatedResponse by lazy {
                routes.first().let {
                    logI(LOG_CATEGORY) {
                        "parsing ${it.responseUuid}"
                    }
                    parser.parse(
                        DirectionsResponseToParse(
                            it.responseJsonRef,
                            it.requestUri,
                            routerOrigin = it.routerOrigin.mapToSdkRouteOrigin(),
                            responseOriginAPI = it.mapboxAPI.mapToSDKResponseOriginAPI(),
                        ),
                    )
                }.getOrThrow()
            }
            routes.map {
                existingParsedRoutesLookup(it.routeId) ?: it.toNavigationRoute(
                    responseTimeElapsedSeconds,
                    routesFromAssociatedResponse,
                )
            }
        }
        .flatten()
        .sortedBy { routes.indexOf(it.nativeRoute) }
}

@OptIn(ExperimentalMapboxNavigationAPI::class)
private fun RouteInterface.toNavigationRoute(
    responseTimeElapsedSeconds: Long,
    parsedRoutes: DirectionsResponseParsingResult,
): NavigationRoute {
    val refreshTtl =
        parsedRoutes.routesParsingResult.getOrNull(routeIndex)?.data?.route?.refreshTtl()
    val routeOptions = parsedRoutes.routeOptions
    return NavigationRoute(
        routeOptions = routeOptions,
        // TODO: test that route options are the same as with direct parsing
        directionsRoute = parsedRoutes.routesParsingResult[routeIndex].data.route,
        waypoints = parsedRoutes.routesParsingResult[routeIndex].data.routesWaypoint,
        nativeRoute = this,
        expirationTimeElapsedSeconds = refreshTtl?.plus(responseTimeElapsedSeconds),
        // TODO: adopt native parsing NAVAND-1732 to prevent response origin API being lost here
        // when existing route is reparsed
        responseOriginAPI = parsedRoutes.routesParsingResult[routeIndex].data.responseOriginAPI,
        // TODO: NAVAND-6774, move overriden traffic to native route
        overriddenTraffic = null,
        operations = parsedRoutes.routesParsingResult[routeIndex].operations,
    )
}
