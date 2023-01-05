package com.mapbox.navigation.ui.app.internal.controller

import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.ui.app.internal.Action
import com.mapbox.navigation.ui.app.internal.State
import com.mapbox.navigation.ui.app.internal.Store
import com.mapbox.navigation.ui.app.internal.destination.DestinationAction
import com.mapbox.navigation.ui.app.internal.extension.FetchRouteCancelled
import com.mapbox.navigation.ui.app.internal.extension.FetchRouteError
import com.mapbox.navigation.ui.app.internal.extension.actionsFlowable
import com.mapbox.navigation.ui.app.internal.extension.dispatch
import com.mapbox.navigation.ui.app.internal.extension.fetchRoute
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import com.mapbox.navigation.ui.app.internal.navigation.NavigationStateAction
import com.mapbox.navigation.ui.app.internal.routefetch.RouteOptionsProvider
import com.mapbox.navigation.ui.app.internal.routefetch.RoutePreviewAction
import com.mapbox.navigation.ui.app.internal.routefetch.RoutePreviewState
import com.mapbox.navigation.ui.app.internal.startActiveNavigation
import com.mapbox.navigation.utils.internal.ifNonNull
import com.mapbox.navigation.utils.internal.logE
import com.mapbox.navigation.utils.internal.toPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch

class RoutePreviewStateController(
    private val store: Store,
    private val routeOptionsProvider: RouteOptionsProvider
) : StateController() {
    init {
        store.register(this)
    }

    private var mapboxNavigation: MapboxNavigation? = null

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)
        this.mapboxNavigation = mapboxNavigation

        coroutineScope.launch { mapboxNavigation.fetchRouteSaga() }
        coroutineScope.launch { fetchRouteAndMoveToNavigationStateSaga() }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        this.mapboxNavigation = null
    }

    override fun process(state: State, action: Action): State {
        if (action is RoutePreviewAction && mapboxNavigation != null) {
            return state.copy(
                previewRoutes = processRoutesAction(state.previewRoutes, action)
            )
        }
        return state
    }

    private fun processRoutesAction(
        state: RoutePreviewState,
        action: RoutePreviewAction
    ): RoutePreviewState {
        return when (action) {
            is RoutePreviewAction.FetchRoute -> {
                RoutePreviewState.Fetching(0)
            }
            is RoutePreviewAction.StartedFetchRequest -> {
                RoutePreviewState.Fetching(action.requestId)
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
            else -> state
        }
    }

    // This SAGA is responsible for processing route fetch request.
    // It uses MapboxNavigation.requestRoutes() and dispatches relevant 'progress' actions when
    // request starts, cancels, fails or successfully finishes (StartedFetchRequest, Canceled, Failed, Ready)
    private suspend fun MapboxNavigation.fetchRouteSaga() {
        store.actionsFlowable()
            .filter {
                it is RoutePreviewAction.FetchRoute ||
                    it is DestinationAction.SetDestination
            }
            .map {
                when (it) {
                    is RoutePreviewAction.FetchRoute -> routeOptions(this)
                    else -> null
                }
            }
            .collectLatest { routeOptions ->
                if (routeOptions != null) {
                    try {
                        val routes = fetchRoute(routeOptions) { requestId ->
                            store.dispatch(RoutePreviewAction.StartedFetchRequest(requestId))
                        }
                        store.dispatch(RoutePreviewAction.Ready(routes))
                    } catch (e: FetchRouteError) {
                        store.dispatch(RoutePreviewAction.Failed(e.reasons, e.routeOptions))
                    } catch (e: FetchRouteCancelled) {
                        store.dispatch(RoutePreviewAction.Canceled(e.routeOptions, e.routerOrigin))
                    } catch (e: Throwable) {
                        logE("Error fetching route. ${e.message}", "RoutesStateController")
                        store.dispatch(RoutePreviewAction.Ready(emptyList()))
                    }
                }
            }
    }

    // This SAGA dispatches FetchRoute action, awaits RoutePreviewState.Ready
    // and updates NavigationState to either RoutePreview or ActiveNavigation state.
    private suspend fun fetchRouteAndMoveToNavigationStateSaga() {
        store.actionsFlowable()
            .filter {
                it is RoutePreviewAction.FetchRouteAndShowRoutePreview ||
                    it is RoutePreviewAction.FetchRouteAndStartActiveNavigation ||
                    it is NavigationStateAction.Update
            }
            .collectLatest {
                when (it) {
                    is RoutePreviewAction.FetchRouteAndShowRoutePreview -> {
                        if (fetchRouteIfNeeded()) {
                            store.dispatch(
                                NavigationStateAction.Update(NavigationState.RoutePreview)
                            )
                        }
                    }
                    is RoutePreviewAction.FetchRouteAndStartActiveNavigation -> {
                        if (fetchRouteIfNeeded()) {
                            val previewRoutes = store.state.value.previewRoutes
                            if (previewRoutes is RoutePreviewState.Ready) {
                                store.dispatch(startActiveNavigation(previewRoutes.routes))
                            }
                        }
                    }
                }
            }
    }

    private suspend fun fetchRouteIfNeeded(): Boolean {
        val storeState = store.state.value
        if (storeState.previewRoutes is RoutePreviewState.Ready) return true
        if (storeState.previewRoutes is RoutePreviewState.Fetching) return false

        return ifNonNull(
            storeState.location?.enhancedLocation?.toPoint(),
            storeState.destination?.point
        ) { _, _ ->
            store.dispatch(RoutePreviewAction.FetchRoute)
            waitWhileFetching()
            store.state.value.previewRoutes is RoutePreviewState.Ready
        } ?: false
    }

    private suspend fun waitWhileFetching() {
        store.select { it.previewRoutes }.takeWhile { it is RoutePreviewState.Fetching }.collect()
    }

    private fun routeOptions(mapboxNavigation: MapboxNavigation): RouteOptions? {
        val state = store.state.value
        return ifNonNull(state.location, state.destination) { location, destination ->
            routeOptionsProvider.getOptions(
                mapboxNavigation,
                location.enhancedLocation.toPoint(),
                destination.point
            )
        }
    }
}
