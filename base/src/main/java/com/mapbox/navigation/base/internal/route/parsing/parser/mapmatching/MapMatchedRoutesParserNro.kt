package com.mapbox.navigation.base.internal.route.parsing.parser.mapmatching

import androidx.annotation.WorkerThread
import com.mapbox.annotation.MapboxExperimental
import com.mapbox.api.directions.v5.models.DirectionsRouteFBWrapper
import com.mapbox.api.directions.v5.models.DirectionsWaypoint
import com.mapbox.api.directions.v5.models.DirectionsWaypointFBWrapper
import com.mapbox.api.directions.v5.models.FBDirectionsRouteContext
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.api.directions.v5.models.utils.FlatbuffersListWrapper
import com.mapbox.directions.route.DirectionsRouteContext
import com.mapbox.directions.route.DirectionsRouteResponse
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.internal.performance.PerformanceTracker
import com.mapbox.navigation.base.internal.route.operations.MapMatchedRouteOperations
import com.mapbox.navigation.base.internal.route.operations.NroRouteOperations
import com.mapbox.navigation.base.internal.route.parsing.ResponseToParse
import com.mapbox.navigation.base.internal.route.parsing.models.DirectionsParsedRouteData
import com.mapbox.navigation.base.internal.route.parsing.models.mapmaptching.MapMatchedResponseParsingResult
import com.mapbox.navigation.base.internal.route.parsing.models.mapmaptching.MapMatchedRouteModelParsingResult
import com.mapbox.navigation.base.internal.route.parsing.models.mapmaptching.MapMatchedRoutesParser
import com.mapbox.navigation.base.internal.route.parsing.models.mapmaptching.ParsedMatchedRouteData
import com.mapbox.navigation.base.route.DirectionsResponseParsingException
import com.mapbox.navigation.base.route.ResponseOriginAPI
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.utils.internal.asIntOrNull
import com.mapbox.navigation.utils.internal.logD
import java.net.URL

internal class MapMatchedRoutesParserNro : MapMatchedRoutesParser {

    override fun parse(response: ResponseToParse): Result<MapMatchedResponseParsingResult> {
        return Result.runCatching {
            logD(LOG_CATEGORY) {
                "Parsing map-matched response"
            }
            PerformanceTracker.trackPerformanceSync(
                "NROMapMatchedModelsParser#parseMapMatchedResponseNRO",
            ) {
                parseMapMatchedResponseNRO(response)
            }
        }
    }

    @OptIn(ExperimentalMapboxNavigationAPI::class, MapboxExperimental::class)
    private companion object {

        const val LOG_CATEGORY = "NRO-MAP-MATCHED-ROUTES_MODELS-PARSER"

        const val KEY_MATCHINGS_INDEX = "matchings_index"
        const val KEY_WAYPOINT_INDEX = "waypoint_index"

        @WorkerThread
        private fun parseMapMatchedResponseNRO(
            response: ResponseToParse,
        ): MapMatchedResponseParsingResult {
            val routeOptions = RouteOptions.fromUrl(URL(response.routeRequest))
            val parsingResult = DirectionsRouteResponse.parseMapMatchingJson(
                response.responseBody,
                routeOptions.geometries(),
            )

            if (parsingResult.isError) {
                throw DirectionsResponseParsingException(
                    Throwable(parsingResult.error ?: "unknown error"),
                )
            }

            val matches = parsingResult.value!!.mapIndexed { index, context ->
                context.toMatchedRouteModelsParsingResult(
                    matchingIndex = index,
                    routeOptions = routeOptions,
                    routerOrigin = response.routerOrigin,
                    responseOriginApi = response.responseOriginAPI,
                )
            }

            return MapMatchedResponseParsingResult(
                matches,
                routeOptions,
                matches.firstOrNull()?.data?.directionsData?.requestUUID,
            )
        }

        fun DirectionsRouteContext.toMatchedRouteModelsParsingResult(
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
            ) ?: throw IllegalStateException("matching returned by parseMapMatchingJson is null")
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
                operations = MapMatchedRouteOperations(NroRouteOperations(this, directionsData)),
            )
        }

        fun getTracepointsFromMMResponse(
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
    }
}
