@file:OptIn(ExperimentalMapboxNavigationAPI::class)

package com.mapbox.navigation.base.internal.route.operations

import com.mapbox.bindgen.DataRef
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI

internal class MapMatchedRouteOperations(
    val javaRouteOperations: JavaRouteOperations,
) : RouteOperations by javaRouteOperations {
    override fun refresh(
        refreshResponse: DataRef,
        legIndex: Int,
        legGeometryIndex: Int,
        responseTimeElapsedSeconds: Long,
    ): Result<RouteUpdate> {
        return Result.failure(
            NotImplementedError(
                "MapMatchedRouteOperations does not support refresh",
            ),
        )
    }
}
