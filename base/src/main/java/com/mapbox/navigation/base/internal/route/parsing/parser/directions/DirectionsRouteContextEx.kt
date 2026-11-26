@file:OptIn(ExperimentalMapboxNavigationAPI::class)

package com.mapbox.navigation.base.internal.route.parsing.parser.directions

import com.mapbox.annotation.MapboxExperimental
import com.mapbox.api.directions.v5.models.DirectionsRouteFBWrapper
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.directions.route.DirectionsRouteContext
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.internal.route.operations.NroRouteOperations
import com.mapbox.navigation.base.internal.route.parsing.models.DirectionsParsedRouteData
import com.mapbox.navigation.base.internal.route.parsing.models.directions.DirectionsRouteModelParsingResult
import com.mapbox.navigation.base.route.ResponseOriginAPI
import com.mapbox.navigation.base.route.RouterOrigin

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
        routesWaypoint = route.waypoints()?.filterNotNull()
            ?: route.contextWaypoints()?.filterNotNull(),
        requestUUID = route.contextUuid,
        routeOptions = routeOptions,
        routeIndex = route.contextRouteIndex,
        routerOrigin = routerOrigin,
        responseOriginAPI = responseOriginApi,
    )
    return DirectionsRouteModelParsingResult(
        data,
        operations = NroRouteOperations(data),
    )
}
