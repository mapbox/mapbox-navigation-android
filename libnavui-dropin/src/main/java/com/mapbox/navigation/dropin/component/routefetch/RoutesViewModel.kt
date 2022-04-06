package com.mapbox.navigation.dropin.component.routefetch

import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.internal.extensions.flowRoutesUpdated
import com.mapbox.navigation.dropin.lifecycle.UIViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

sealed class RoutesAction {
    data class FetchPoints(val points: List<Point>) : RoutesAction()
    data class FetchOptions(val options: RouteOptions) : RoutesAction()
    data class SetRoutes(val routes: List<NavigationRoute>) : RoutesAction()
    data class Ready(val routes: List<NavigationRoute>) : RoutesAction()
    data class Failed(
        val reasons: List<RouterFailure>,
        val routeOptions: RouteOptions
    ) : RoutesAction()
    data class Canceled(
        val routeOptions: RouteOptions,
        val routerOrigin: RouterOrigin
    ) : RoutesAction()
}

internal class RoutesViewModel : UIViewModel<RoutesState, RoutesAction>(RoutesState.Empty) {

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        mainJobControl.scope.launch {
            mapboxNavigation.flowRoutesUpdated().collect { result ->
                // Empty is ignored on purpose. When the action is processed
                // it will be converted to RoutesState.Empty.
                invoke(RoutesAction.Ready(result.navigationRoutes))
            }
        }
    }

    override fun process(
        mapboxNavigation: MapboxNavigation,
        state: RoutesState,
        action: RoutesAction
    ): RoutesState {
        return when (action) {
            is RoutesAction.FetchPoints -> {
                val routeOptions = getDefaultOptions(mapboxNavigation, action.points)
                val requestId = mapboxNavigation.fetchRoute(routeOptions)
                RoutesState.Fetching(requestId)
            }
            is RoutesAction.FetchOptions -> {
                val requestId = mapboxNavigation.fetchRoute(action.options)
                RoutesState.Fetching(requestId)
            }
            is RoutesAction.SetRoutes -> {
                mapboxNavigation.setNavigationRoutes(action.routes)
                if (action.routes.isEmpty()) {
                    RoutesState.Empty
                } else {
                    RoutesState.Ready(action.routes)
                }
            }
            is RoutesAction.Ready -> {
                if (action.routes.isEmpty()) {
                    RoutesState.Empty
                } else {
                    RoutesState.Ready(action.routes)
                }
            }
            is RoutesAction.Canceled -> {
                RoutesState.Canceled(action.routeOptions, action.routerOrigin)
            }
            is RoutesAction.Failed -> {
                RoutesState.Failed(action.reasons, action.routeOptions)
            }
        }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        when (val currentState = state.value) {
            is RoutesState.Fetching -> {
                mapboxNavigation.cancelRouteRequest(currentState.requestId)
            }
        }
    }

    private fun MapboxNavigation.fetchRoute(options: RouteOptions): Long {
        return requestRoutes(
            options,
            object : NavigationRouterCallback {
                override fun onRoutesReady(
                    routes: List<NavigationRoute>,
                    routerOrigin: RouterOrigin
                ) {
                    invoke(RoutesAction.SetRoutes(routes))
                }

                override fun onFailure(
                    reasons: List<RouterFailure>,
                    routeOptions: RouteOptions
                ) {
                    invoke(RoutesAction.Failed(reasons, routeOptions))
                }

                override fun onCanceled(routeOptions: RouteOptions, routerOrigin: RouterOrigin) {
                    invoke(RoutesAction.Canceled(routeOptions, routerOrigin))
                }
            }
        )
    }

    private fun getDefaultOptions(
        mapboxNavigation: MapboxNavigation,
        points: List<Point>
    ): RouteOptions {
        return RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .applyLanguageAndVoiceUnitOptions(mapboxNavigation.navigationOptions.applicationContext)
            .layersList(listOf(mapboxNavigation.getZLevel(), null))
            .coordinatesList(points)
            .alternatives(true)
            .build()
    }
}
