package com.mapbox.navigation.dropin.controller

import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.component.routefetch.RoutesAction
import com.mapbox.navigation.dropin.component.routefetch.RoutesState
import com.mapbox.navigation.dropin.internal.extensions.flowRoutesUpdated
import com.mapbox.navigation.dropin.model.Action
import com.mapbox.navigation.dropin.model.State
import com.mapbox.navigation.dropin.model.Store
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@ExperimentalPreviewMapboxNavigationAPI
internal class RoutesStateController(
    private val store: Store
) : StateController() {
    init {
        store.register(this)
    }

    private var mapboxNavigation: MapboxNavigation? = null

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)
        this.mapboxNavigation = mapboxNavigation

        coroutineScope.launch {
            mapboxNavigation.flowRoutesUpdated().collect { result ->
                // Empty is ignored on purpose. When the action is processed
                // it will be converted to RoutesState.Empty.
                store.dispatch(RoutesAction.Ready(result.navigationRoutes))
            }
        }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        when (val currentState = store.state.value.routes) {
            is RoutesState.Fetching -> {
                mapboxNavigation.cancelRouteRequest(currentState.requestId)
            }
            else -> Unit
        }
        this.mapboxNavigation = null
    }

    override fun process(state: State, action: Action): State {
        if (action is RoutesAction) {
            return this.mapboxNavigation?.let {
                return state.copy(
                    routes = processRoutesAction(it, state.routes, action)
                )
            } ?: state
        }
        return state
    }

    private fun processRoutesAction(
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

    private fun MapboxNavigation.fetchRoute(options: RouteOptions): Long {
        return requestRoutes(
            options,
            object : NavigationRouterCallback {
                override fun onRoutesReady(
                    routes: List<NavigationRoute>,
                    routerOrigin: RouterOrigin
                ) {
                    store.dispatch(RoutesAction.SetRoutes(routes))
                }

                override fun onFailure(
                    reasons: List<RouterFailure>,
                    routeOptions: RouteOptions
                ) {
                    store.dispatch(RoutesAction.Failed(reasons, routeOptions))
                }

                override fun onCanceled(routeOptions: RouteOptions, routerOrigin: RouterOrigin) {
                    store.dispatch(RoutesAction.Canceled(routeOptions, routerOrigin))
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
