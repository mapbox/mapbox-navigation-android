package com.mapbox.navigation.ui.maps.internal.ui

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.extensions.flowRouteAlternativeObserver
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import com.mapbox.navigation.ui.utils.internal.Provider
import com.mapbox.navigation.utils.internal.ifNonNull
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

interface RouteAlternativeContract {
    fun onAlternativeRoutesUpdated(
        legIndex: Int,
        mapboxNavigation: MapboxNavigation,
        updatedRoutes: List<NavigationRoute>
    )
}

internal class MapboxRouteAlternativeComponentContract : RouteAlternativeContract {
    override fun onAlternativeRoutesUpdated(
        legIndex: Int,
        mapboxNavigation: MapboxNavigation,
        updatedRoutes: List<NavigationRoute>
    ) {
        mapboxNavigation.setNavigationRoutes(
            routes = updatedRoutes,
            initialLegIndex = legIndex
        )
    }
}

class RouteAlternativeComponent(
    private val provider: Provider<RouteAlternativeContract>? = null
) : UIComponent() {

    private val contractProvider: Provider<RouteAlternativeContract> =
        this.provider ?: Provider {
            MapboxRouteAlternativeComponentContract()
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
                        isPrimaryRouteOffboard || offBoardAlternatives.isNotEmpty() -> {
                            updatedRoutes.add(routePrimary)
                            updatedRoutes.addAll(offBoardAlternatives)
                        }
                        else -> {
                            updatedRoutes.add(routePrimary)
                            updatedRoutes.addAll(alternatives)
                        }
                    }
                    val currentLegIndex = routeProgress.currentLegProgress?.legIndex ?: 0
                    contractProvider.get().onAlternativeRoutesUpdated(
                        legIndex = currentLegIndex,
                        mapboxNavigation = mapboxNavigation,
                        updatedRoutes = updatedRoutes
                    )
                }
            }
        }
    }
}
