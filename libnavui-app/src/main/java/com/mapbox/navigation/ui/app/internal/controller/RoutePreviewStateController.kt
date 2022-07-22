package com.mapbox.navigation.ui.app.internal.controller

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
import com.mapbox.navigation.ui.app.internal.Action
import com.mapbox.navigation.ui.app.internal.State
import com.mapbox.navigation.ui.app.internal.Store
import com.mapbox.navigation.ui.app.internal.routefetch.RoutePreviewAction
import com.mapbox.navigation.ui.app.internal.routefetch.RoutePreviewState

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class RoutePreviewStateController(private val store: Store) : StateController() {
    init {
        store.register(this)
    }

    private var mapboxNavigation: MapboxNavigation? = null

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)
        this.mapboxNavigation = mapboxNavigation
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        when (val currentState = store.state.value.previewRoutes) {
            is RoutePreviewState.Fetching -> {
                mapboxNavigation.cancelRouteRequest(currentState.requestId)
            }
            else -> Unit
        }
        this.mapboxNavigation = null
    }

    override fun process(state: State, action: Action): State {
        if (action is RoutePreviewAction) {
            return this.mapboxNavigation?.let {
                return state.copy(
                    previewRoutes = processRoutesAction(it, action)
                )
            } ?: state
        }
        return state
    }

    private fun processRoutesAction(
        mapboxNavigation: MapboxNavigation,
        action: RoutePreviewAction
    ): RoutePreviewState {
        return when (action) {
            is RoutePreviewAction.FetchPoints -> {
                val routeOptions = getDefaultOptions(mapboxNavigation, action.points)
                val requestId = mapboxNavigation.fetchRoute(routeOptions)
                RoutePreviewState.Fetching(requestId)
            }
            is RoutePreviewAction.FetchOptions -> {
                val requestId = mapboxNavigation.fetchRoute(action.options)
                RoutePreviewState.Fetching(requestId)
            }
            is RoutePreviewAction.Ready -> {
                if (action.routes.isEmpty()) {
                    RoutePreviewState.Empty
                } else {
                    RoutePreviewState.Ready(action.routes)
                }
            }
            is RoutePreviewAction.Canceled -> {
                RoutePreviewState.Canceled(action.routeOptions, action.routerOrigin)
            }
            is RoutePreviewAction.Failed -> {
                RoutePreviewState.Failed(action.reasons, action.routeOptions)
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
                    store.dispatch(RoutePreviewAction.Ready(routes))
                }

                override fun onFailure(
                    reasons: List<RouterFailure>,
                    routeOptions: RouteOptions
                ) {
                    store.dispatch(RoutePreviewAction.Failed(reasons, routeOptions))
                }

                override fun onCanceled(routeOptions: RouteOptions, routerOrigin: RouterOrigin) {
                    store.dispatch(RoutePreviewAction.Canceled(routeOptions, routerOrigin))
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
