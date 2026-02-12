@file:OptIn(ExperimentalMapboxNavigationAPI::class)

package com.mapbox.navigation.base.internal.route.parsing.models

import androidx.annotation.WorkerThread
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
import com.mapbox.navigation.base.internal.route.parsing.DirectionsResponseToParse
import com.mapbox.navigation.base.route.DirectionsResponseParsingException
import com.mapbox.navigation.base.route.ResponseOriginAPI
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.utils.internal.LoggerFrontend
import com.mapbox.navigation.utils.internal.logD
import java.net.URL

private const val LOG_CATEGORY = "NRO-ROUTE-MODELS-PARSER"

internal class NRORouteModelsParser(
    private val logger: LoggerFrontend,
) : RouteModelsParser {
    override fun parse(
        response: DirectionsResponseToParse,
    ): Result<DirectionsResponseParsingResult> {
        logger.logD(LOG_CATEGORY) {
            "parsing directions response"
        }
        return Result.runCatching {
            PerformanceTracker.trackPerformanceSync(
                "NRORouteModelsParser#parseDirectionsResponseNRO",
            ) {
                parseDirectionsResponseNRO(response)
            }
        }
    }
}

@WorkerThread
private fun parseDirectionsResponseNRO(
    directionsResponseToParse: DirectionsResponseToParse,
): DirectionsResponseParsingResult {
    val routeOptions = RouteOptions.fromUrl(URL(directionsResponseToParse.routeRequest))
    val parsingResult = DirectionsRouteResponse.parseDirectionsRoutesJson(
        directionsResponseToParse.responseBody,
    )
    if (parsingResult.isError) {
        throw DirectionsResponseParsingException(
            Throwable(parsingResult.error ?: "unknown error"),
        )
    }
    val routes = parsingResult.value!!.map {
        it.toRouteModelsParsingResult(
            routeOptions,
            directionsResponseToParse.routerOrigin,
            directionsResponseToParse.responseOriginAPI,
        )
    }
    return DirectionsResponseParsingResult(
        routes,
        routeOptions,
        routes.firstOrNull()?.data?.requestUUID,
    )
}

internal fun DirectionsRouteContext.toRouteModelsParsingResult(
    routeOptions: RouteOptions,
    @RouterOrigin routerOrigin: String,
    @ResponseOriginAPI responseOriginApi: String,
): RouteModelParsingResult {
    val route = DirectionsRouteFBWrapper.wrap(
        routeOptions = routeOptions,
        bindgenContext = this,
    ) ?: throw IllegalStateException("route returned by getRootAsDirectionsRouteContext is null")
    val data = ParsedRouteData(
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
    return RouteModelParsingResult(
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
