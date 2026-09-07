@file:OptIn(ExperimentalMapboxNavigationAPI::class)

package com.mapbox.navigation.base.internal.route.operations

import com.mapbox.bindgen.DataRef
import com.mapbox.directions.route.DirectionsRouteContext
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI

internal class MapMatchedRouteOperations(
    val routeOperations: RouteOperations,
) : RouteOperations by routeOperations {
    override fun refresh(
        refreshResponse: DataRef,
        legIndex: Int,
        legGeometryIndex: Int,
        responseTimeElapsedSeconds: Long,
        refreshedContext: DirectionsRouteContext,
    ): Result<RouteUpdate> {
        return Result.failure(
            NotImplementedError(
                "MapMatchedRouteOperations does not support refresh",
            ),
        )
    }
}
