@file:OptIn(ExperimentalMapboxNavigationAPI::class, MapboxExperimental::class)

package com.mapbox.navigation.base.internal.route.parsing.parser.nn

import com.mapbox.annotation.MapboxExperimental
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.internal.route.parsing.models.nn.ContinuousAlternativesParsingSuccessfulResult
import com.mapbox.navigation.base.internal.route.parsing.models.nn.RouteInterfacesParser
import com.mapbox.navigation.base.internal.route.parsing.parser.directions.toRouteModelsParsingResult
import com.mapbox.navigation.base.internal.utils.AlternativesParsingResult
import com.mapbox.navigation.base.internal.utils.mapToSDKResponseOriginAPI
import com.mapbox.navigation.base.internal.utils.mapToSdkRouteOrigin
import com.mapbox.navigation.base.internal.utils.refreshTtl
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.utils.internal.Time
import com.mapbox.navigation.utils.internal.logE
import com.mapbox.navigator.RouteInterface
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.net.URL

private const val LOG_CATEGORY = "NroRouteInterfacesParser"

/**
 * Alternative to [JsonResponseOptimizedRouteInterfaceParser] for the case when NRO
 * ([com.mapbox.navigation.base.options.NavigationOptions.Builder.nativeRouteObject]) is enabled.
 *
 * The [RouteInterface]s handed to [parserContinuousAlternatives] are already fully parsed by NN,
 * so instead of re-parsing their [RouteInterface.getResponseJsonRef] JSON (as
 * [JsonResponseOptimizedRouteInterfaceParser] does via
 * [com.mapbox.navigation.base.internal.route.parsing.parser.directions.DirectionsRoutesParserNro])
 * this implementation builds the NRO route model directly off the
 * [com.mapbox.directions.route.DirectionsRouteContext] already held by each [RouteInterface]
 * ([RouteInterface.getDirectionsRouteContext]).
 */
internal class NroRouteInterfacesParser(
    private val parsingDispatcher: CoroutineDispatcher,
    private val time: Time,
) : RouteInterfacesParser {
    override suspend fun parserContinuousAlternatives(
        routes: List<RouteInterface>,
    ): AlternativesParsingResult<Result<ContinuousAlternativesParsingSuccessfulResult>> {
        val responseTimeElapsedSeconds = time.seconds()

        return withContext(parsingDispatcher) {
            Result.runCatching {
                routes.map { it.toNavigationRouteFromNativeContext(responseTimeElapsedSeconds) }
            }.onFailure {
                logE(LOG_CATEGORY) { "Alternative route parsing failed: ${it.message}" }
            }.map {
                ContinuousAlternativesParsingSuccessfulResult(it)
            }
        }.let { AlternativesParsingResult.Parsed(it) }
    }
}

private fun RouteInterface.toNavigationRouteFromNativeContext(
    responseTimeElapsedSeconds: Long,
): NavigationRoute {
    val routeOptions = RouteOptions.fromUrl(URL(requestUri))
    val result = directionsRouteContext.toRouteModelsParsingResult(
        routeOptions = routeOptions,
        routerOrigin = routerOrigin.mapToSdkRouteOrigin(),
        responseOriginApi = mapboxAPI.mapToSDKResponseOriginAPI(),
    )
    return NavigationRoute(
        routeOptions = routeOptions,
        directionsRoute = result.data.route,
        waypoints = result.data.routesWaypoint,
        nativeRoute = this,
        expirationTimeElapsedSeconds = result.data.route.refreshTtl()
            ?.plus(responseTimeElapsedSeconds),
        responseOriginAPI = result.data.responseOriginAPI,
        overriddenTraffic = null,
        operations = result.operations,
        directionsRouteContext = directionsRouteContext,
    )
}
