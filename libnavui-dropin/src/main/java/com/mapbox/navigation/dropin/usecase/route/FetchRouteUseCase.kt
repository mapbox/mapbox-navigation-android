package com.mapbox.navigation.dropin.usecase.route

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.extensions.coroutines.requestRoutes
import com.mapbox.navigation.dropin.usecase.UseCase
import com.mapbox.navigation.dropin.usecase.location.GetCurrentLocationUseCase
import com.mapbox.navigation.utils.internal.toPoint
import kotlinx.coroutines.CoroutineDispatcher

internal class FetchRouteUseCase(
    private val navigation: MapboxNavigation,
    private val routeOptionsBuilder: () -> RouteOptions.Builder,
    private val getCurrentLocation: GetCurrentLocationUseCase,
    dispatcher: CoroutineDispatcher
) : UseCase<Point, List<DirectionsRoute>>(dispatcher) {

    override suspend fun execute(destination: Point): List<DirectionsRoute> {
        val origin = getCurrentLocation(Unit).getOrNull() ?: return emptyList()

        val routeOptions = routeOptionsBuilder()
            .coordinatesList(listOf(origin.toPoint(), destination))
            .layersList(listOf(navigation.getZLevel(), null))
            .alternatives(true)
            .build()

        return navigation.requestRoutes(routeOptions).routes
    }
}
