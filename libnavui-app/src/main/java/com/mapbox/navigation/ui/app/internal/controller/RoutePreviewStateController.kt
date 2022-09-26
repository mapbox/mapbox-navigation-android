package com.mapbox.navigation.ui.app.internal.controller

import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.ui.app.internal.Action
import com.mapbox.navigation.ui.app.internal.State
import com.mapbox.navigation.ui.app.internal.Store
import com.mapbox.navigation.ui.app.internal.destination.DestinationAction
import com.mapbox.navigation.ui.app.internal.extension.actionsFlowable
import com.mapbox.navigation.ui.app.internal.routefetch.RoutePreviewAction
import com.mapbox.navigation.ui.app.internal.routefetch.RoutePreviewState
import com.mapbox.navigation.utils.internal.logE
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class RoutePreviewStateController(private val store: Store) : StateController() {
    init {
        store.register(this)
    }

    private var mapboxNavigation: MapboxNavigation? = null

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)
        this.mapboxNavigation = mapboxNavigation

        coroutineScope.launch {
            mapboxNavigation.fetchRouteSaga()
        }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        this.mapboxNavigation = null
    }

    override fun process(state: State, action: Action): State {
        if (action is RoutePreviewAction && mapboxNavigation != null) {
            return state.copy(
                previewRoutes = processRoutesAction(action)
            )
        }
        return state
    }

    private fun processRoutesAction(action: RoutePreviewAction): RoutePreviewState {
        return when (action) {
            is RoutePreviewAction.FetchOptions -> {
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
        }
    }

    private suspend fun MapboxNavigation.fetchRouteSaga() {
        store.actionsFlowable()
            .filter {
                it is RoutePreviewAction.FetchOptions ||
                    it is DestinationAction.SetDestination
            }
            .map {
                when (it) {
                    is RoutePreviewAction.FetchOptions -> it.options
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

    private suspend fun MapboxNavigation.fetchRoute(
        routeOptions: RouteOptions,
        fetchStarted: (requestId: Long) -> Unit
    ): List<NavigationRoute> {
        return suspendCancellableCoroutine { cont ->
            val requestId = requestRoutes(
                routeOptions,
                object : NavigationRouterCallback {
                    override fun onRoutesReady(
                        routes: List<NavigationRoute>,
                        routerOrigin: RouterOrigin
                    ) {
                        cont.resume(routes)
                    }

                    override fun onFailure(
                        reasons: List<RouterFailure>,
                        routeOptions: RouteOptions
                    ) {
                        cont.resumeWithException(FetchRouteError(reasons, routeOptions))
                    }

                    override fun onCanceled(
                        routeOptions: RouteOptions,
                        routerOrigin: RouterOrigin
                    ) {
                        cont.cancel(FetchRouteCancelled(routeOptions, routerOrigin))
                    }
                }
            )
            fetchStarted(requestId)
            cont.invokeOnCancellation { cancelRouteRequest(requestId) }
        }
    }

    private class FetchRouteError(
        val reasons: List<RouterFailure>,
        val routeOptions: RouteOptions
    ) : Error()

    private class FetchRouteCancelled(
        val routeOptions: RouteOptions,
        val routerOrigin: RouterOrigin
    ) : Error()
}
