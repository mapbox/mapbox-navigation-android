package com.mapbox.navigation.core.fasterroute

import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArraySet

@ExperimentalMapboxNavigationAPI
fun MapboxNavigation.createFasterRoutes(
    options: FasterRouteOptions
) = FasterRoutes(
    this,
    FasterRouteTracker(options),
    Dispatchers.Main
)

@ExperimentalMapboxNavigationAPI
class FasterRoutes internal constructor(
    private val mapboxNavigation: MapboxNavigation,
    private val fasterRouteTracker: FasterRouteTracker,
    mainDispatcher: CoroutineDispatcher
) {

    private val scope = CoroutineScope(SupervisorJob() + mainDispatcher)
    private var previousRouteUpdateProcessing: Job? = null
    private val newFasterRoutesObservers = CopyOnWriteArraySet<NewFasterRouteObserver>()

    private val internalObserver = RoutesObserver { result: RoutesUpdatedResult ->
        previousRouteUpdateProcessing?.cancel()
        previousRouteUpdateProcessing = scope.launch {
            val fasterRouteTrackerResult = fasterRouteTracker.routesUpdated(
                result,
                mapboxNavigation.getAlternativeMetadataFor(result.navigationRoutes)
            )
            when (fasterRouteTrackerResult) {
                is FasterRouteResult.NewFasterRoadFound -> newFasterRouteFound(
                    NewFasterRoute(
                        fasterRouteTrackerResult.route,
                        fasterRouteTrackerResult.fasterThanPrimary
                    )
                )
                FasterRouteResult.NoFasterRoad -> {}
            }
        }
    }

    init {
        mapboxNavigation.registerRoutesObserver(internalObserver)
    }

    fun registerNewFasterRouteObserver(observer: NewFasterRouteObserver) {
        newFasterRoutesObservers.add(observer)
    }

    fun unregisterNewFasterRouteObserver(observer: NewFasterRouteObserver) {
        newFasterRoutesObservers.remove(observer)
    }

    fun destroy() {
        mapboxNavigation.unregisterRoutesObserver(internalObserver)
        scope.cancel()
    }

    private fun newFasterRouteFound(newFasterRoute: NewFasterRoute) {
        newFasterRoutesObservers.forEach { it.onNewFasterRouteFound(newFasterRoute) }
    }
}
