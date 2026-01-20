@file:OptIn(ExperimentalMapboxNavigationAPI::class)

package com.mapbox.navigation.base.internal.route.parsing.models

import androidx.annotation.WorkerThread
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.DirectionsWaypoint
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.internal.route.operations.RouteOperations
import com.mapbox.navigation.base.internal.route.parsing.DirectionsResponseToParse
import com.mapbox.navigation.base.route.ResponseOriginAPI
import com.mapbox.navigation.base.route.RouterOrigin

/**
 * Enty point for low level parsing, used as abstraction over the following parsing implementations:
 * - [NRORouteModelsParser]
 * - [JavaRouteModelsParser]
 */
internal fun interface RouteModelsParser {
    @WorkerThread
    fun parse(
        response: DirectionsResponseToParse,
    ): Result<DirectionsResponseParsingResult>
}

internal data class DirectionsResponseParsingResult(
    val routesParsingResult: List<RouteModelParsingResult>,
    val routeOptions: RouteOptions,
    val responseUUID: String?,
)

internal data class RouteModelParsingResult(
    val data: ParsedRouteData,
    val operations: RouteOperations,
)

internal data class ParsedRouteData(
    val route: DirectionsRoute,
    val routesWaypoint: List<DirectionsWaypoint>?,
    val requestUUID: String?,
    val routeOptions: RouteOptions,
    val routeIndex: Int,
    @RouterOrigin val routerOrigin: String,
    @ResponseOriginAPI val responseOriginAPI: String,
)
