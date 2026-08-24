@file:OptIn(ExperimentalMapboxNavigationAPI::class)

package com.mapbox.navigation.base.internal.route.parsing.parser.directions

import androidx.annotation.WorkerThread
import com.mapbox.annotation.MapboxExperimental
import com.mapbox.api.directions.v5.models.DirectionsRouteFBWrapper
import com.mapbox.api.directions.v5.models.DirectionsWaypoint
import com.mapbox.api.directions.v5.models.DirectionsWaypointFBWrapper
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.api.directions.v5.models.utils.FlatbuffersListWrapper
import com.mapbox.directions.route.DirectionsRouteContext
import com.mapbox.directions.route.DirectionsRouteResponse
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.internal.performance.PerformanceTracker
import com.mapbox.navigation.base.internal.route.operations.NroRouteOperations
import com.mapbox.navigation.base.internal.route.parsing.ResponseToParse
import com.mapbox.navigation.base.internal.route.parsing.models.DirectionsParsedRouteData
import com.mapbox.navigation.base.internal.route.parsing.models.directions.DirectionsResponseParsingResult
import com.mapbox.navigation.base.internal.route.parsing.models.directions.DirectionsRouteModelParsingResult
import com.mapbox.navigation.base.internal.route.parsing.models.directions.DirectionsRoutesParser
import com.mapbox.navigation.base.route.DirectionsResponseParsingException
import com.mapbox.navigation.base.route.ResponseOriginAPI
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.utils.internal.LoggerFrontend
import com.mapbox.navigation.utils.internal.logD
import java.net.URL

private const val LOG_CATEGORY = "NRO-ROUTE-MODELS-PARSER"

internal class DirectionsRoutesParserNro(
    private val logger: LoggerFrontend,
) : DirectionsRoutesParser {
    override fun parse(
        response: ResponseToParse,
    ): Result<DirectionsResponseParsingResult> {
        return Result.runCatching {
            logger.logD(LOG_CATEGORY) { "parsing directions response" }
            PerformanceTracker.trackPerformanceSync(
                "NRORouteModelsParser#parseDirectionsResponseNRO",
            ) {
                parseDirectionsResponseNRO(response)
            }
        }
    }
}

@OptIn(MapboxExperimental::class)
@WorkerThread
private fun parseDirectionsResponseNRO(
    responseToParse: ResponseToParse,
): DirectionsResponseParsingResult {
    val routeOptions = RouteOptions.fromUrl(URL(responseToParse.routeRequest))
    val parsingResult = DirectionsRouteResponse.parseDirectionsRoutesJson(
        responseToParse.responseBody,
    )
    if (parsingResult.isError) {
        throw DirectionsResponseParsingException(
            Throwable(parsingResult.error ?: "unknown error"),
        )
    }
    val routes = parsingResult.value!!.map {
        it.toRouteModelsParsingResult(
            routeOptions,
            responseToParse.routerOrigin,
            responseToParse.responseOriginAPI,
        )
    }
    return DirectionsResponseParsingResult(
        routes,
        routeOptions,
        routes.firstOrNull()?.data?.requestUUID,
    )
}

@OptIn(MapboxExperimental::class)
internal fun DirectionsRouteContext.toRouteModelsParsingResult(
    routeOptions: RouteOptions,
    @RouterOrigin routerOrigin: String,
    @ResponseOriginAPI responseOriginApi: String,
): DirectionsRouteModelParsingResult {
    val route = DirectionsRouteFBWrapper.wrap(
        routeOptions = routeOptions,
        bindgenContext = this,
    ) ?: throw IllegalStateException("route returned by getRootAsDirectionsRouteContext is null")
    val data = DirectionsParsedRouteData(
        route = route,
        routesWaypoint = route.waypoints()?.filterNotNull() ?: getWaypointsFromResponse(
            route.fbContext,
        ),
        requestUUID = route.fbContext.uuid,
        routeOptions = routeOptions,
        routeIndex = route.fbContext.route.routeIndex.toInt(),
        routerOrigin = routerOrigin,
        responseOriginAPI = responseOriginApi,
    )
    return DirectionsRouteModelParsingResult(
        data,
        operations = NroRouteOperations(this, data),
    )
}

private fun getWaypointsFromResponse(
    routeContext: com.mapbox.directions.generated.DirectionsRouteContext,
): List<DirectionsWaypoint>? =
    FlatbuffersListWrapper.get(routeContext.waypointsLength) {
        DirectionsWaypointFBWrapper.wrap(routeContext.waypoints(it)) as? DirectionsWaypoint
    }?.filterNotNull()
