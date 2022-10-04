package com.mapbox.navigation.core.fasterroute

import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.utils.internal.ThreadController
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@ExperimentalMapboxNavigationAPI
class FasterRoutes internal constructor(
    private val mapboxNavigation: MapboxNavigation,
    private val fasterRouteTracker: FasterRouteTracker,
    private val computationDispatcher: CoroutineDispatcher,
    private val mainDispatcher: CoroutineDispatcher
) : RoutesObserver {

    private val scope = CoroutineScope(SupervisorJob() + computationDispatcher)
    private var previousRouteUpdateProcessing: Job? = null

    init {
        mapboxNavigation.registerRoutesObserver(this)
    }

    var fasterRouteCallback: NewFasterRouteCallback = NewFasterRouteCallback { }

    override fun onRoutesChanged(result: RoutesUpdatedResult) {
        previousRouteUpdateProcessing?.cancel()
        previousRouteUpdateProcessing = scope.launch {
            val fasterRouteTrackerResult = fasterRouteTracker.routesUpdated(
                result,
                mapboxNavigation.getAlternativeMetadataFor(result.navigationRoutes)
            )
            launch(mainDispatcher) {
                when (fasterRouteTrackerResult) {
                    is FasterRouteResult.NewFasterRoadFound -> fasterRouteCallback.onNewFasterRouteFound(
                        NewFasterRoute(
                            fasterRouteTrackerResult.route,
                            fasterRouteTrackerResult.fasterThanPrimary
                        )
                    )
                    FasterRouteResult.NoFasterRoad -> {}
                }
            }
        }
    }

    fun destroy() {
        mapboxNavigation.unregisterRoutesObserver(this)
        scope.cancel()
    }
}

class NewFasterRoute(
    val fasterRoute: NavigationRoute,
    val fasterThanPrimary: Double
)

fun interface NewFasterRouteCallback {
    fun onNewFasterRouteFound(newFasterRoute: NewFasterRoute)
}

@ExperimentalMapboxNavigationAPI
fun MapboxNavigation.createFasterRoutes(
    options: FasterRouteOptions
) = FasterRoutes(
    this,
    FasterRouteTracker(options),
    ThreadController.DefaultDispatcher,
    Dispatchers.Main
)
