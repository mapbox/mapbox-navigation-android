package com.mapbox.navigation.base.internal.route.parsing.parser.mapmatching

import androidx.annotation.WorkerThread
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.api.matching.v5.models.MapMatchingResponse
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.internal.performance.PerformanceTracker
import com.mapbox.navigation.base.internal.route.operations.JavaRouteOperations
import com.mapbox.navigation.base.internal.route.operations.MapMatchedRouteOperations
import com.mapbox.navigation.base.internal.route.parsing.ResponseToParse
import com.mapbox.navigation.base.internal.route.parsing.models.DirectionsParsedRouteData
import com.mapbox.navigation.base.internal.route.parsing.models.mapmaptching.MapMatchedResponseParsingResult
import com.mapbox.navigation.base.internal.route.parsing.models.mapmaptching.MapMatchedRouteModelParsingResult
import com.mapbox.navigation.base.internal.route.parsing.models.mapmaptching.MapMatchedRoutesParser
import com.mapbox.navigation.base.internal.route.parsing.models.mapmaptching.ParsedMatchedRouteData
import com.mapbox.navigation.base.internal.route.parsing.parser.getDirectionsRoute
import com.mapbox.navigation.base.internal.route.parsing.parser.getDirectionsWaypoint
import com.mapbox.navigation.base.internal.route.toDirectionsResponse
import com.mapbox.navigation.base.internal.utils.toByteArray
import com.mapbox.navigation.base.route.ResponseOriginAPI
import com.mapbox.navigation.base.route.RouterOrigin
import java.net.URL

internal class MapMatchedRoutesParserJava : MapMatchedRoutesParser {
    override fun parse(
        response: ResponseToParse,
    ): Result<MapMatchedResponseParsingResult> {
        return Result.runCatching {
            PerformanceTracker.trackPerformanceSync(
                "MapMatchedRouteModelsParser#parse",
            ) {
                parseMapMatchedResponseJava(response)
            }
        }
    }
}

@WorkerThread
private fun parseMapMatchedResponseJava(
    response: ResponseToParse,
): MapMatchedResponseParsingResult {
    val routeOptions = RouteOptions.fromUrl(URL(response.routeRequest))
    val responseString = String(response.responseBody.toByteArray(), Charsets.UTF_8)
    val model = MapMatchingResponse.fromJson(responseString)
    val directionsResponse = model.toDirectionsResponse(routeOptions)
    val confidences = model.matchings()?.map { it.confidence() } ?: emptyList()
    return createMapMatchingResponseParsingResult(
        directionsResponse,
        routeOptions,
        response.routerOrigin,
        response.responseOriginAPI,
        confidences,
    )
}

@OptIn(ExperimentalMapboxNavigationAPI::class)
private fun createMapMatchingResponseParsingResult(
    response: DirectionsResponse,
    routeOptions: RouteOptions,
    @RouterOrigin routerOrigin: String,
    @ResponseOriginAPI responseOriginAPI: String,
    confidences: List<Double>,
): MapMatchedResponseParsingResult = MapMatchedResponseParsingResult(
    response.routes().mapIndexed { index, _ ->
        val route = response.getDirectionsRoute(index, routeOptions)
        val waypoints = response.getDirectionsWaypoint(index)
        val directionsData = DirectionsParsedRouteData(
            route,
            waypoints,
            response.uuid(),
            routeOptions,
            routeIndex = index,
            routerOrigin = routerOrigin,
            responseOriginAPI = responseOriginAPI,
        )
        MapMatchedRouteModelParsingResult(
            ParsedMatchedRouteData(
                directionsData = directionsData,
                mapMatchingConfidence = confidences.getOrNull(index),
            ),
            MapMatchedRouteOperations(JavaRouteOperations(directionsData, null)),
        )
    },
    routeOptions,
    response.uuid(),
)
