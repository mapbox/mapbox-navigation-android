package com.mapbox.navigation.ui.maps.internal.route.line

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.maps.route.line.api.RoutesRenderedResult

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
object RoutesRenderedResultFactory {

    fun routesRenderedResult(
        successfullyRenderedRouteIds: Set<String>,
        renderingCancelledRouteIds: Set<String>,
        successfullyClearedRouteIds: Set<String>,
        clearingCancelledRouteIds: Set<String>,
    ): RoutesRenderedResult = RoutesRenderedResult(
        successfullyRenderedRouteIds,
        renderingCancelledRouteIds,
        successfullyClearedRouteIds,
        clearingCancelledRouteIds
    )
}
