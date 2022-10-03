package com.mapbox.navigation.core.internal.fasterroute

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.utils.internal.ThreadController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class FasterRoutes(
    options: FasterRouteOptions,
    private val mapboxNavigation: MapboxNavigation
) : RoutesObserver {

    private val scope = CoroutineScope(SupervisorJob() + ThreadController.DefaultDispatcher)
    private val fasterRouteTracker = FasterRouteTracker(options)
    private var previousRouteUpdateProcessing: Job? = null

    init {
        mapboxNavigation.registerRoutesObserver(this)
    }

    var onNewFasterRouteAvailable: (NewFasterRoute) -> Unit = { }

    override fun onRoutesChanged(result: RoutesUpdatedResult) {
        previousRouteUpdateProcessing?.cancel()
        previousRouteUpdateProcessing = scope.launch {
            val fasterRouteTrackerResult = fasterRouteTracker.routesUpdated(
                result,
                mapboxNavigation.getAlternativeMetadataFor(result.navigationRoutes)
            )
            launch(Dispatchers.Main) {
                when (fasterRouteTrackerResult) {
                    is FasterRouteResult.NewFasterRoadFound -> onNewFasterRouteAvailable(
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