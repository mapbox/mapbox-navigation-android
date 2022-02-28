package com.mapbox.navigation.dropin.usecase.route

import com.mapbox.geojson.Point
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.di.DispatcherMain
import com.mapbox.navigation.dropin.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

internal class FetchAndSetRouteUseCase @Inject constructor(
    private val navigation: MapboxNavigation,
    private val findRoutesUseCase: FetchRouteUseCase,
    @DispatcherMain dispatcher: CoroutineDispatcher
) : UseCase<Point, Unit>(dispatcher) {

    override suspend fun execute(destination: Point) {
        val routes = findRoutesUseCase(destination).getOrDefault(emptyList())
        navigation.setRoutes(routes)
    }
}
