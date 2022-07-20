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
import com.mapbox.navigation.core.internal.extensions.flowRoutesUpdated
import com.mapbox.navigation.ui.app.internal.Action
import com.mapbox.navigation.ui.app.internal.State
import com.mapbox.navigation.ui.app.internal.Store
import com.mapbox.navigation.ui.app.internal.SuspendAction
import com.mapbox.navigation.ui.app.internal.SuspendReducer
import com.mapbox.navigation.ui.app.internal.routefetch.RoutesAction
import com.mapbox.navigation.ui.app.internal.routefetch.RoutesState
import com.mapbox.navigation.ui.app.internal.routefetch.RoutesSuspendAction
import com.mapbox.navigation.ui.utils.internal.ifNonNull
import com.mapbox.navigation.utils.internal.toPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class RoutesStateController(
    private val store: Store
) : StateController(), SuspendReducer {

    init {
        store.register(this)
        store.registerSuspend(this)
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

    override suspend fun process(state: State, action: SuspendAction): State {
        if (action is RoutesSuspendAction) {
            return processRoutesSuspendAction(action)
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

    private suspend fun processRoutesSuspendAction(
        action: RoutesSuspendAction
    ): State {
        return when (action) {
            is RoutesSuspendAction.RequestCurrent -> {
                requestCurrentRoute()
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

    /**
     * Dispatch FetchPoints action and wait for RoutesState.Ready.
     * Method returns immediately if already in RoutesState.Ready or RoutesState.Fetching, or if
     * required location or destination data is missing.
     *
     * @return the state once the route state has completed the fetching state
     */
    private suspend fun requestCurrentRoute(): State {
        val storeState = store.state.value
        if (storeState.routes is RoutesState.Ready) return storeState
        if (storeState.routes is RoutesState.Fetching) return storeState

        ifNonNull(
            storeState.location?.enhancedLocation?.toPoint(),
            storeState.destination
        ) { lastPoint, destination ->
            store.dispatch(RoutesAction.FetchPoints(listOf(lastPoint, destination.point)))
            store.select { it.routes }.takeWhile { it is RoutesState.Fetching }.collect()
        }
        return store.state.value
    }
}
