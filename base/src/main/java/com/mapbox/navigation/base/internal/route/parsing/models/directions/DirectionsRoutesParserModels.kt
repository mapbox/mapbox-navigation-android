package com.mapbox.navigation.base.internal.route.parsing.models.directions

import androidx.annotation.WorkerThread
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.internal.route.operations.RouteOperations
import com.mapbox.navigation.base.internal.route.parsing.ResponseToParse
import com.mapbox.navigation.base.internal.route.parsing.models.DirectionsParsedRouteData

/**
 * Enty point for low level parsing, used as abstraction over the following parsing implementations:
 * - [com.mapbox.navigation.base.internal.route.parsing.parser.directions.DirectionsRoutesParserNro]
 * - [com.mapbox.navigation.base.internal.route.parsing.parser.directions.DirectionsRoutesParserJava]
 */
internal fun interface DirectionsRoutesParser {
    @WorkerThread
    fun parse(
        response: ResponseToParse,
    ): Result<DirectionsResponseParsingResult>
}

internal data class DirectionsResponseParsingResult(
    val routesParsingResult: List<DirectionsRouteModelParsingResult>,
    val routeOptions: RouteOptions,
    val responseUUID: String?,
)

internal data class DirectionsRouteModelParsingResult(
    val data: DirectionsParsedRouteData,
    val operations: RouteOperations,
)
