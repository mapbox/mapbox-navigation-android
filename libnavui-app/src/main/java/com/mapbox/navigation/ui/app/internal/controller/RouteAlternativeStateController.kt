package com.mapbox.navigation.ui.app.internal.controller

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.extensions.flowRouteAlternativeObserver
import com.mapbox.navigation.ui.app.internal.Action
import com.mapbox.navigation.ui.app.internal.State
import com.mapbox.navigation.ui.app.internal.Store
import com.mapbox.navigation.ui.app.internal.routefetch.RoutesAction
import com.mapbox.navigation.utils.internal.ifNonNull
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class RouteAlternativeStateController(private val store: Store) : StateController() {
    init {
        store.register(this)
    }

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        coroutineScope.launch {
            mapboxNavigation.flowRouteAlternativeObserver().collect { pair ->
                val routeProgress = pair.first
                val alternatives = pair.second
                val currentRoutes = mapboxNavigation.getNavigationRoutes()
                val primaryRoute = currentRoutes.firstOrNull()
                ifNonNull(primaryRoute) { routePrimary ->
                    val isPrimaryRouteOffboard = routePrimary.origin == RouterOrigin.Offboard
                    val offBoardAlternatives = alternatives.filter {
                        it.origin == RouterOrigin.Offboard
                    }
                    val updatedRoutes = mutableListOf<NavigationRoute>()
                    when {
                        isPrimaryRouteOffboard -> {
                            updatedRoutes.add(routePrimary)
                            updatedRoutes.addAll(offBoardAlternatives)
                        }
                        isPrimaryRouteOffboard.not() && offBoardAlternatives.isNotEmpty() -> {
                            updatedRoutes.addAll(offBoardAlternatives)
                        }
                        else -> {
                            updatedRoutes.add(routePrimary)
                            updatedRoutes.addAll(alternatives)
                        }
                    }
                    val currentLegIndex = routeProgress.currentLegProgress?.legIndex ?: 0
                    store.dispatch(
                        RoutesAction.SetRoutesWithIndex(updatedRoutes, currentLegIndex)
                    )
                }
            }
        }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
    }

    override fun process(state: State, action: Action): State = state
}
