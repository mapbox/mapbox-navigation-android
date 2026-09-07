@file:OptIn(ExperimentalMapboxNavigationAPI::class, MapboxExperimental::class)

package com.mapbox.navigation.base.internal.route.parsing.parser.directions

import com.mapbox.annotation.MapboxExperimental
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.internal.SDKRouteParser
import com.mapbox.navigation.base.internal.performance.PerformanceTracker
import com.mapbox.navigation.base.internal.route.RoutesResponse
import com.mapbox.navigation.base.internal.route.parsing.ResponseToParse
import com.mapbox.navigation.base.internal.route.parsing.RouteParsingTracking
import com.mapbox.navigation.base.internal.route.parsing.models.directions.DirectionsResponseParsingResult
import com.mapbox.navigation.base.internal.route.parsing.models.directions.NavigationRouteParsingSuccessfulResult
import com.mapbox.navigation.base.internal.route.parsing.models.directions.NavigationRoutesParser
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.utils.internal.LoggerFrontend
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigation.utils.internal.Time
import com.mapbox.navigation.utils.internal.logD
import com.mapbox.navigation.utils.internal.runCatchingSuspend
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.net.URL
import kotlin.time.Duration.Companion.milliseconds

private const val LOG_CATEGORY = "NRO-FROM-NATIVE-ROUTE-PARSING"
private const val PERFORMANCE_SECTION_NAME =
    "NroFromNativeRouteNavigationRoutesParser#parseResponse"

/**
 * Alternative to [NnAndModelsParallelNavigationRoutesParser] for the case when NRO
 * ([com.mapbox.navigation.base.options.NavigationOptions.Builder.nativeRouteObject]) is enabled.
 *
 * Instead of independently parsing the response twice (once natively into [RouteInterface] via
 * [SDKRouteParser], once more via [DirectionsRoutesParserNro] into a [DirectionsRouteFBWrapper]
 * based on a second, unrelated native parse of the same JSON), this implementation parses the
 * response natively only once and builds the NRO route model directly from the
 * [com.mapbox.directions.route.DirectionsRouteContext] already held by the resulting
 * [com.mapbox.navigator.RouteInterface]
 * ([com.mapbox.navigator.RouteInterface.getDirectionsRouteContext]).
 */
internal class NroFromNativeRouteNavigationRoutesParser(
    private val routeParsingTracking: RouteParsingTracking,
    private val parsingDispatcher: CoroutineDispatcher =
        ThreadController.DefaultDispatcher,
    private val time: Time = Time.SystemClockImpl,
    private val nnParser: SDKRouteParser,
    private val logger: LoggerFrontend,
) : NavigationRoutesParser {

    override suspend fun parseDirectionsResponse(
        response: ResponseToParse,
    ): Result<NavigationRouteParsingSuccessfulResult> {
        val responseTimeElapsedMillis = time.millis()
        return withContext(parsingDispatcher) {
            parseFromNativeRoute(
                response = response,
                responseTimeElapsedMillis = responseTimeElapsedMillis,
                time = time,
                logger = logger,
                routeParsingTracking = routeParsingTracking,
                nnParser = nnParser,
            )
        }
    }
}

internal suspend fun parseFromNativeRoute(
    response: ResponseToParse,
    responseTimeElapsedMillis: Long,
    time: Time,
    logger: LoggerFrontend,
    routeParsingTracking: RouteParsingTracking,
    nnParser: SDKRouteParser,
): Result<NavigationRouteParsingSuccessfulResult> = runCatchingSuspend {
    logger.logD(LOG_CATEGORY) {
        "parsing directions response"
    }

    val parsedResponse = PerformanceTracker.trackPerformanceAsync<RoutesResponse>(
        PERFORMANCE_SECTION_NAME,
    ) {
        val startElapsedMillis = time.millis()
        val waitMillis = startElapsedMillis - responseTimeElapsedMillis

        val nativeParse = PerformanceTracker.trackPerformanceSync(
            "SDKRouteParser#parseDirectionsResponse()",
        ) {
            nnParser.parseDirectionsResponse(
                response.responseBody,
                response.routeRequest,
                response.routerOrigin,
            )
        }

        val nativeRoutes = nativeParse.value
            ?: throw IllegalStateException(
                "failed to parse response natively: ${nativeParse.error}",
            )
        val parseMillis = time.millis() - startElapsedMillis
        val parseThread = Thread.currentThread().name

        logger.logD(LOG_CATEGORY) {
            "parsed directions response to RouteInterface " +
                "for ${nativeRoutes.firstOrNull()?.responseUuid}, " +
                "parse time ${parseMillis}ms"
        }

        val routeOptions = RouteOptions.fromUrl(URL(response.routeRequest))
        val routesParsingResult = nativeRoutes.map { routeInterface ->
            routeInterface.getDirectionsRouteContext().toRouteModelsParsingResult(
                routeOptions = routeOptions,
                routerOrigin = response.routerOrigin,
                responseOriginApi = response.responseOriginAPI,
            )
        }
        val directionsResponseParsingResult = DirectionsResponseParsingResult(
            routesParsingResult,
            routeOptions,
            routesParsingResult.firstOrNull()?.data?.requestUUID,
        )

        val routes = NavigationRoute.create(
            nativeParse,
            directionsResponseParsingResult,
            responseTimeElapsedMillis.milliseconds.inWholeSeconds,
            response.responseOriginAPI,
        )

        logger.logD(LOG_CATEGORY) {
            "$PERFORMANCE_SECTION_NAME " +
                "for ${routes.firstOrNull()?.responseUUID}," +
                "total parse time ${parseMillis}ms"
        }

        RoutesResponse(
            routes = routes,
            meta = RoutesResponse.Metadata(
                createdAtElapsedMillis = time.millis(),
                responseWaitMillis = waitMillis,
                responseParseMillis = parseMillis,
                responseParseThread = parseThread,
                nativeWaitMillis = waitMillis,
                nativeParseMillis = parseMillis,
            ),
        )
    }

    if (parsedResponse.routes.isEmpty()) {
        throw IllegalStateException("no routes returned, collection is empty")
    }
    routeParsingTracking.routeResponseIsParsed(
        parsedResponse.meta,
    )
    NavigationRouteParsingSuccessfulResult(
        parsedResponse.routes,
    )
}.onFailure {
    logger.logE("Route parsing failed: ${it.message}", LOG_CATEGORY)
}
