@file:OptIn(ExperimentalMapboxNavigationAPI::class)

package com.mapbox.navigation.base.internal.route.parsing.models

import androidx.annotation.WorkerThread
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.DirectionsWaypoint
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.internal.route.operations.JavaRouteOperations
import com.mapbox.navigation.base.internal.route.parsing.DirectionsResponseToParse
import com.mapbox.navigation.base.route.ResponseOriginAPI
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.utils.internal.LoggerFrontend
import com.mapbox.navigation.utils.internal.LoggerProvider
import com.mapbox.navigation.utils.internal.logD
import com.mapbox.navigation.utils.internal.toReader
import java.net.URL

private const val LOG_CATEGORY = "JAVA-ROUTE-MODELS-PARSER"

internal class JavaRouteModelsParser(
    private val logger: LoggerFrontend = LoggerProvider.getLoggerFrontend(),
) : RouteModelsParser {
    override fun parse(
        response: DirectionsResponseToParse,
    ): Result<DirectionsResponseParsingResult> {
        logger.logD(LOG_CATEGORY) {
            "parsing directions response"
        }
        return Result.runCatching {
            parseDirectionsResponseJava(response)
        }
    }
}

@WorkerThread
internal fun parseDirectionsResponseJava(
    responseToParse: DirectionsResponseToParse,
): DirectionsResponseParsingResult {
    val routeOptions = RouteOptions.fromUrl(URL(responseToParse.routeRequest))
    val response = responseToParse.responseBody.toReader().use { reader ->
        DirectionsResponse.fromJson(reader)
    }
    return createResponseParsingResult(
        response,
        routeOptions,
        responseToParse.routerOrigin,
        responseToParse.responseOriginAPI,
    )
}

internal fun createResponseParsingResult(
    response: DirectionsResponse,
    routeOptions: RouteOptions,
    @RouterOrigin routerOrigin: String,
    @ResponseOriginAPI responseOriginAPI: String,
): DirectionsResponseParsingResult = DirectionsResponseParsingResult(
    response.routes().mapIndexed { index, route ->
        val route = getDirectionsRoute(response, index, routeOptions)
        val waypoints = getDirectionsWaypoint(response, index)
        val routeData = ParsedRouteData(
            route,
            waypoints,
            response.uuid(),
            routeOptions,
            routeIndex = index,
            routerOrigin = routerOrigin,
            responseOriginAPI = responseOriginAPI,
        )
        RouteModelParsingResult(
            routeData,
            JavaRouteOperations(
                routeData,
                null,
            ),
        )
    },
    routeOptions,
    response.uuid(),
)

private fun getDirectionsRoute(
    response: DirectionsResponse,
    routeIndex: Int,
    routeOptions: RouteOptions,
): DirectionsRoute {
    return response.routes()[routeIndex].toBuilder()
        .requestUuid(response.uuid())
        .routeIndex(routeIndex.toString())
        .routeOptions(routeOptions)
        .build()
}

private fun getDirectionsWaypoint(
    directionsResponse: DirectionsResponse,
    routeIndex: Int,
): List<DirectionsWaypoint>? {
    return directionsResponse.routes()[routeIndex].waypoints() ?: directionsResponse.waypoints()
}
