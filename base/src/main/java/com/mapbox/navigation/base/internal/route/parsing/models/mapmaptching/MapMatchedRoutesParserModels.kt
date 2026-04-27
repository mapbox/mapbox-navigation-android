package com.mapbox.navigation.base.internal.route.parsing.models.mapmaptching

import androidx.annotation.WorkerThread
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.internal.route.operations.RouteOperations
import com.mapbox.navigation.base.internal.route.parsing.ResponseToParse
import com.mapbox.navigation.base.internal.route.parsing.models.DirectionsParsedRouteData

internal fun interface MapMatchedRoutesParser {

    @WorkerThread
    fun parse(response: ResponseToParse): Result<MapMatchedResponseParsingResult>
}

internal data class MapMatchedResponseParsingResult(
    val routesParsingResult: List<MapMatchedRouteModelParsingResult>,
    val routeOptions: RouteOptions,
    val responseUUID: String?,
)

internal data class MapMatchedRouteModelParsingResult(
    val data: ParsedMatchedRouteData,
    val operations: RouteOperations,
)

internal data class ParsedMatchedRouteData(
    val directionsData: DirectionsParsedRouteData,
    val mapMatchingConfidence: Double? = null,
)
