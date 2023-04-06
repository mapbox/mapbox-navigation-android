package com.mapbox.navigation.dropin.map

import com.mapbox.geojson.Point
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.ClickBehavior
import com.mapbox.navigation.ui.app.internal.Store
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import com.mapbox.navigation.ui.app.internal.routefetch.RoutePreviewAction
import com.mapbox.navigation.ui.app.internal.routefetch.RoutePreviewState
import com.mapbox.navigation.ui.app.internal.routefetch.RoutesAction
import com.mapbox.navigation.ui.maps.internal.ui.RouteLineComponentContract
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

internal class RouteLineComponentContractImpl(
    private val store: Store,
    private val mapClickBehavior: ClickBehavior<Point>,
) : RouteLineComponentContract {
    override fun setRoutes(
        mapboxNavigation: MapboxNavigation,
        routes: List<NavigationRoute>,
        initialLegIndex: Int?
    ) {
        when (store.state.value.navigation) {
            is NavigationState.RoutePreview -> {
                store.dispatch(RoutePreviewAction.Ready(routes))
            }
            is NavigationState.ActiveNavigation -> {
                val action = if (initialLegIndex == null) {
                    RoutesAction.SetRoutes(routes)
                } else {
                    RoutesAction.SetRoutes(routes, initialLegIndex)
                }
                store.dispatch(action)
            }
            else -> {
                // no op
            }
        }
    }

    override fun getRouteInPreview(): Flow<List<NavigationRoute>?> {
        return combine(
            store.select { it.navigation },
            store.select { it.previewRoutes },
        ) { navigationState, routePreviewState ->
            if (routePreviewState is RoutePreviewState.Ready) {
                if (navigationState == NavigationState.RoutePreview) {
                    routePreviewState.routes
                } else {
                    null
                }
            } else {
                emptyList()
            }
        }
    }

    override fun onMapClicked(point: Point) {
        mapClickBehavior.onClicked(point)
    }
}
