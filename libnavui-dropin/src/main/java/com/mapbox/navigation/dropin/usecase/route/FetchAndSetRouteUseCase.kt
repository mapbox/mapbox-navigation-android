package com.mapbox.navigation.dropin.usecase.route

import com.mapbox.geojson.Point
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher

internal class FetchAndSetRouteUseCase(
    private val navigation: MapboxNavigation,
    private val findRoutesUseCase: FetchRouteUseCase,
    dispatcher: CoroutineDispatcher
) : UseCase<Point, Unit>(dispatcher) {

    override suspend fun execute(destination: Point) {
        val routes = findRoutesUseCase(destination).getOrDefault(emptyList())
        navigation.setRoutes(routes)
    }
}
