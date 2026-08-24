package com.mapbox.navigation.base.internal.route.parsing.parser.directions

import androidx.annotation.WorkerThread
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.internal.performance.PerformanceTracker
import com.mapbox.navigation.base.internal.route.operations.JavaRouteOperations
import com.mapbox.navigation.base.internal.route.parsing.ResponseToParse
import com.mapbox.navigation.base.internal.route.parsing.models.DirectionsParsedRouteData
import com.mapbox.navigation.base.internal.route.parsing.models.directions.DirectionsResponseParsingResult
import com.mapbox.navigation.base.internal.route.parsing.models.directions.DirectionsRouteModelParsingResult
import com.mapbox.navigation.base.internal.route.parsing.models.directions.DirectionsRoutesParser
import com.mapbox.navigation.base.internal.route.parsing.parser.getDirectionsRoute
import com.mapbox.navigation.base.internal.route.parsing.parser.getDirectionsWaypoint
import com.mapbox.navigation.base.route.ResponseOriginAPI
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.utils.internal.LoggerFrontend
import com.mapbox.navigation.utils.internal.LoggerProvider
import com.mapbox.navigation.utils.internal.logD
import com.mapbox.navigation.utils.internal.toReader
import java.net.URL

private const val LOG_CATEGORY = "JAVA-ROUTE-MODELS-PARSER"

internal class DirectionsRoutesParserJava(
    private val logger: LoggerFrontend = LoggerProvider.getLoggerFrontend(),
) : DirectionsRoutesParser {

    @OptIn(ExperimentalMapboxNavigationAPI::class)
    override fun parse(
        response: ResponseToParse,
    ): Result<DirectionsResponseParsingResult> {
        return Result.runCatching {
            logger.logD(LOG_CATEGORY) { "parsing directions response" }
            PerformanceTracker.trackPerformanceSync(
                "JavaRouteModelsParser#parseDirectionsResponseJava",
            ) {
                parseDirectionsResponseJava(response)
            }
        }
    }
}

@WorkerThread
private fun parseDirectionsResponseJava(
    responseToParse: ResponseToParse,
): DirectionsResponseParsingResult {
    val routeOptions = RouteOptions.fromUrl(URL(responseToParse.routeRequest))
    val response = responseToParse.responseBody.toReader().use { reader ->
        DirectionsResponse.fromJson(reader)
    }
    return createResponseParsingResult(
        response,
        routeOptions,
        responseToParse.routerOrigin,
    )
}

@OptIn(ExperimentalMapboxNavigationAPI::class)
internal fun createResponseParsingResult(
    response: DirectionsResponse,
    routeOptions: RouteOptions,
    @RouterOrigin routerOrigin: String,
): DirectionsResponseParsingResult = DirectionsResponseParsingResult(
    response.routes().mapIndexed { index, route ->
        val route = response.getDirectionsRoute(index, routeOptions)
        val waypoints = response.getDirectionsWaypoint(index)
        val routeData = DirectionsParsedRouteData(
            route,
            waypoints,
            response.uuid(),
            routeOptions,
            routeIndex = index,
            routerOrigin = routerOrigin,
            responseOriginAPI = ResponseOriginAPI.DIRECTIONS_API,
        )
        DirectionsRouteModelParsingResult(
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
