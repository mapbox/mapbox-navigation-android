package com.mapbox.navigation.dropin.usecase.route

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.di.DispatcherIo
import com.mapbox.navigation.dropin.extensions.coroutines.requestRoutes
import com.mapbox.navigation.dropin.usecase.UseCase
import com.mapbox.navigation.dropin.usecase.location.GetCurrentLocationUseCase
import com.mapbox.navigation.utils.internal.toPoint
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider

internal class FetchRouteUseCase @Inject constructor(
    private val navigation: MapboxNavigation,
    @Named("fetchRoute")
    private val routeOptionsBuilderProvider: Provider<RouteOptions.Builder>,
    private val getCurrentLocation: GetCurrentLocationUseCase,
    @DispatcherIo dispatcher: CoroutineDispatcher
) : UseCase<Point, List<DirectionsRoute>>(dispatcher) {

    override suspend fun execute(destination: Point): List<DirectionsRoute> {
        val origin = getCurrentLocation(Unit).getOrNull() ?: return emptyList()

        val routeOptions = routeOptionsBuilderProvider.get()
            .coordinatesList(listOf(origin.toPoint(), destination))
            .layersList(listOf(navigation.getZLevel(), null))
            .alternatives(true)
            .build()

        return navigation.requestRoutes(routeOptions).routes
    }
}
