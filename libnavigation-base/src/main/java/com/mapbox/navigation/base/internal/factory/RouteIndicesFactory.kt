package com.mapbox.navigation.base.internal.factory

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.internal.trip.model.RouteIndices

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
object RouteIndicesFactory {

    fun buildRouteIndices(
        legIndex: Int,
        stepIndex: Int,
        routeGeometryIndex: Int,
        legGeometryIndex: Int,
        intersectionIndex: Int,
    ): RouteIndices = RouteIndices(
        legIndex,
        stepIndex,
        routeGeometryIndex,
        legGeometryIndex,
        intersectionIndex
    )
}
