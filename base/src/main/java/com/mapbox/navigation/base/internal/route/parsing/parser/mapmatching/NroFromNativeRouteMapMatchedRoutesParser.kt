@file:OptIn(ExperimentalMapboxNavigationAPI::class, ExperimentalPreviewMapboxNavigationAPI::class)

package com.mapbox.navigation.base.internal.route.parsing.parser.mapmatching

import com.mapbox.annotation.MapboxExperimental
import com.mapbox.api.directions.v5.models.DirectionsRouteFBWrapper
import com.mapbox.api.directions.v5.models.DirectionsWaypoint
import com.mapbox.api.directions.v5.models.DirectionsWaypointFBWrapper
import com.mapbox.api.directions.v5.models.FBDirectionsRouteContext
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.api.directions.v5.models.utils.FlatbuffersListWrapper
import com.mapbox.directions.route.DirectionsRouteContext
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.internal.SDKRouteParser
import com.mapbox.navigation.base.internal.performance.PerformanceTracker
import com.mapbox.navigation.base.internal.route.RoutesResponse
import com.mapbox.navigation.base.internal.route.operations.MapMatchedRouteOperations
import com.mapbox.navigation.base.internal.route.operations.NroRouteOperations
import com.mapbox.navigation.base.internal.route.parsing.ResponseToParse
import com.mapbox.navigation.base.internal.route.parsing.RouteParsingTracking
import com.mapbox.navigation.base.internal.route.parsing.models.DirectionsParsedRouteData
import com.mapbox.navigation.base.internal.route.parsing.models.mapmaptching.MapMatchedResponseParsingResult
import com.mapbox.navigation.base.internal.route.parsing.models.mapmaptching.MapMatchedRouteModelParsingResult
import com.mapbox.navigation.base.internal.route.parsing.models.mapmaptching.MapMatchingMatchParser
import com.mapbox.navigation.base.internal.route.parsing.models.mapmaptching.MapMatchingMatchParsingSuccessfulResult
import com.mapbox.navigation.base.internal.route.parsing.models.mapmaptching.ParsedMatchedRouteData
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.ResponseOriginAPI
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigation.utils.internal.Time
import com.mapbox.navigation.utils.internal.asIntOrNull
import com.mapbox.navigation.utils.internal.logD
import com.mapbox.navigation.utils.internal.logE
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.net.URL
import kotlin.time.Duration.Companion.milliseconds

@OptIn(MapboxExperimental::class)
internal class NroFromNativeRouteMapMatchedRoutesParser(
    private val routeParsingTracking: RouteParsingTracking,
    private val parsingDispatcher: CoroutineDispatcher = ThreadController.DefaultDispatcher,
    private val time: Time = Time.SystemClockImpl,
    private val nnParser: SDKRouteParser,
) : MapMatchingMatchParser {

    override suspend fun parseMapMatchedResponse(
        response: ResponseToParse,
    ): Result<MapMatchingMatchParsingSuccessfulResult> {
        val responseTimeElapsedMillis = time.millis()
        return withContext(parsingDispatcher) {
            parseFromNativeRoute(response, responseTimeElapsedMillis)
        }
    }

    private fun parseFromNativeRoute(
        response: ResponseToParse,
        responseTimeElapsedMillis: Long,
    ): Result<MapMatchingMatchParsingSuccessfulResult> = runCatching {
        logD(LOG_CATEGORY) { "parsing map matched response" }

        val startElapsedMillis = time.millis()
        val waitMillis = startElapsedMillis - responseTimeElapsedMillis

        val matches = PerformanceTracker.trackPerformanceSync(
            "NroFromNativeRouteMapMatchedRoutesParser#parseMapMatchedResponse",
        ) {
            val nativeParse = PerformanceTracker.trackPerformanceSync(
                "SDKRouteParser#parseMapMatchedResponse()",
            ) {
                nnParser.parseMapMatchedResponse(
                    response.responseBody,
                    response.routeRequest,
                    response.routerOrigin,
                )
            }
            val nativeRoutes = nativeParse.value
                ?: throw IllegalStateException(
                    "failed to parse map matched response natively: ${nativeParse.error}",
                )

            val routeOptions = RouteOptions.fromUrl(URL(response.routeRequest))

            val routesParsingResult = nativeRoutes.mapIndexed { matchingIndex, routeInterface ->
                routeInterface.directionsRouteContext.toMatchedRouteModelsParsingResult(
                    matchingIndex = matchingIndex,
                    routeOptions = routeOptions,
                    routerOrigin = response.routerOrigin,
                    responseOriginApi = response.responseOriginAPI,
                )
            }

            NavigationRoute.createFromMapMatchingResult(
                nativeParse,
                MapMatchedResponseParsingResult(
                    routesParsingResult,
                    routeOptions,
                    routesParsingResult.firstOrNull()?.data?.directionsData?.requestUUID,
                ),
                responseTimeElapsedMillis.milliseconds.inWholeSeconds,
            )
        }

        val parseMillis = time.millis() - startElapsedMillis
        logD(LOG_CATEGORY) {
            "parseMapMatchedResponse for ${matches.firstOrNull()?.navigationRoute?.id}, " +
                "total parse time ${parseMillis}ms"
        }

        if (matches.isEmpty()) {
            throw IllegalStateException("no routes returned, collection is empty")
        }
        routeParsingTracking.routeResponseIsParsed(
            RoutesResponse.Metadata(
                createdAtElapsedMillis = time.millis(),
                responseWaitMillis = waitMillis,
                responseParseMillis = parseMillis,
                responseParseThread = Thread.currentThread().name,
                nativeWaitMillis = waitMillis,
                nativeParseMillis = parseMillis,
            ),
        )
        MapMatchingMatchParsingSuccessfulResult(matches)
    }.onFailure {
        logE("Map matched route parsing failed: ${it.message}", LOG_CATEGORY)
    }

    internal fun DirectionsRouteContext.toMatchedRouteModelsParsingResult(
        matchingIndex: Int,
        routeOptions: RouteOptions,
        @RouterOrigin routerOrigin: String,
        @ResponseOriginAPI responseOriginApi: String,
    ): MapMatchedRouteModelParsingResult {
        val route = DirectionsRouteFBWrapper.wrap(
            routeOptions = routeOptions,
            bindgenContext = this,
            // FIXME(NAVSDKCPP-1438)
            // A matching carries no `waypoints` of its own; its waypoints are the response-level
            // tracepoints which point back at it. Supplying them here keeps both
            // `DirectionsRoute.waypoints()` and `NavigationRoute.waypoints` aligned with the Java
            // model, which synthesizes the same per-route waypoints.
            externalWaypoints = { context ->
                getTracepointsFromMMResponse(context, matchingIndex)
            },
        ) ?: throw IllegalStateException("matching returned by the native parser is null")
        val directionsData = DirectionsParsedRouteData(
            route = route,
            routesWaypoint = route.waypoints()?.filterNotNull(),
            requestUUID = route.fbContext.uuid,
            routeOptions = routeOptions,
            routeIndex = route.fbContext.route.routeIndex.toInt(),
            routerOrigin = routerOrigin,
            responseOriginAPI = responseOriginApi,
        )
        return MapMatchedRouteModelParsingResult(
            ParsedMatchedRouteData(
                directionsData = directionsData,
                mapMatchingConfidence = route.mapMatchingConfidence(),
            ),
            operations = MapMatchedRouteOperations(NroRouteOperations(directionsData)),
        )
    }

    private fun getTracepointsFromMMResponse(
        routeContext: FBDirectionsRouteContext,
        matchingIndex: Int,
    ): List<DirectionsWaypoint?>? {
        return FlatbuffersListWrapper.get(routeContext.waypointsLength) {
            DirectionsWaypointFBWrapper.wrap(routeContext.waypoints(it))
        }
            ?.filterNotNull()
            ?.filter { tracepoint ->
                val properties = tracepoint.unrecognizedJsonProperties
                val belongsToMatchingIndex = properties?.get(KEY_MATCHINGS_INDEX)?.asIntOrNull()
                val waypointIndex = properties?.get(KEY_WAYPOINT_INDEX)?.asIntOrNull()
                belongsToMatchingIndex == matchingIndex && waypointIndex != null
            }
            ?.takeIf { it.isNotEmpty() }
    }

    private companion object {
        const val LOG_CATEGORY = "NRO-FROM-NATIVE-ROUTE-MAP-MATCHED-PARSING"
        const val KEY_MATCHINGS_INDEX = "matchings_index"
        const val KEY_WAYPOINT_INDEX = "waypoint_index"
    }
}
