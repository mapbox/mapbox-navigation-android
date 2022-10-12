package com.mapbox.navigation.core.fasterroute

import androidx.annotation.UiThread
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.utils.internal.logE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArraySet

/***
 * Keeps track of primary and alternatives routes to notify in [NewFasterRouteObserver] that a
 * new faster route is available.
 */
@ExperimentalPreviewMapboxNavigationAPI
class FasterRoutesTracker internal constructor(
    private val mapboxNavigation: MapboxNavigation,
    private val fasterRouteTrackerCore: FasterRouteTrackerCore,
    private val scope: CoroutineScope
) {

    private var previousRouteUpdateProcessing: Job? = null
    private val newFasterRoutesObservers = CopyOnWriteArraySet<NewFasterRouteObserver>()

    private val internalObserver = RoutesObserver { result: RoutesUpdatedResult ->
        previousRouteUpdateProcessing?.cancel()
        previousRouteUpdateProcessing = scope.launch {
            val fasterRouteTrackerResult = fasterRouteTrackerCore.findFasterRouteInUpdate(
                result,
                mapboxNavigation.getAlternativeMetadataFor(result.navigationRoutes)
            )
            when (fasterRouteTrackerResult) {
                is FasterRouteResult.NewFasterRouteFound -> newFasterRouteFound(
                    NewFasterRoute(
                        fasterRouteTrackerResult.route,
                        fasterRouteTrackerResult.fasterThanPrimaryBy,
                        fasterRouteTrackerResult.alternativeId
                    )
                )
                FasterRouteResult.NoFasterRoute -> {}
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

    /***
     * Sets faster route as primary to [MapboxNavigation]
     */
    @UiThread
    fun acceptFasterRoute(newFasterRoute: NewFasterRoute) {
        val currentRoutes = mapboxNavigation.getNavigationRoutes()
        if (currentRoutes.contains(newFasterRoute.fasterRoute)) {
            mapboxNavigation.setNavigationRoutes(
                listOf(newFasterRoute.fasterRoute) +
                    currentRoutes.filterNot { it == newFasterRoute.fasterRoute }
            )
        } else {
            logE("Ignoring accepted faster route as it's not present in current routes")
        }
    }

    /***
     * Remembers faster route as declined to not offer similar to this one again.
     */
    @UiThread
    fun declineFasterRoute(newFasterRoute: NewFasterRoute) {
        fasterRouteTrackerCore.fasterRouteDeclined(
            newFasterRoute.alternativeId,
            newFasterRoute.fasterRoute
        )
    }

    /***
     * Stops observation and processing routes from [MapboxNavigation]
     */
    @UiThread
    fun destroy() {
        mapboxNavigation.unregisterRoutesObserver(internalObserver)
        scope.cancel()
        isDestroyed = true
    }

    internal var isDestroyed = false
        private set

    private fun newFasterRouteFound(newFasterRoute: NewFasterRoute) {
        newFasterRoutesObservers.forEach { it.onNewFasterRouteFound(newFasterRoute) }
    }
}
