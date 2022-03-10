package com.mapbox.navigation.dropin.component.routefetch

import android.util.Log
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
import com.mapbox.navigation.dropin.component.location.LocationViewModel
import com.mapbox.navigation.dropin.component.navigation.NavigationStateViewModel
import com.mapbox.navigation.dropin.component.navigationstate.NavigationState
import com.mapbox.navigation.dropin.lifecycle.UIViewModel
import com.mapbox.navigation.dropin.model.Destination
import kotlinx.coroutines.flow.MutableStateFlow

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class RoutesViewModel(
    private val navigationStateViewModel: NavigationStateViewModel,
    private val locationViewModel: LocationViewModel,
    state: MutableStateFlow<RoutesState> = MutableStateFlow(RoutesState.INITIAL_STATE)
) : UIViewModel<RoutesState, RoutesAction>(state) {

    private var routeRequestId: Long? = null

    override fun process(
        mapboxNavigation: MapboxNavigation,
        state: RoutesState,
        action: RoutesAction
    ): RoutesState {
        when (action) {
            is RoutesAction.SetDestination -> {
                if (shouldReloadRoute(state.destination, action.destination)) {
                    fetchAndSetRoute(mapboxNavigation, action.destination)
                }
                return state.copy(destination = action.destination)
            }
            is RoutesAction.FetchAndSetRoute -> {
                fetchAndSetRoute(mapboxNavigation, state.destination)
            }
            is RoutesAction.FetchPoints -> {
                val routeOptions = getDefaultOptions(mapboxNavigation, action.points)
                fetchRoute(routeOptions, mapboxNavigation)
            }
            is RoutesAction.FetchOptions -> {
                fetchRoute(action.options, mapboxNavigation)
            }
            is RoutesAction.SetRoutes -> {
                mapboxNavigation.setNavigationRoutes(action.routes, action.legIndex)
            }
            is RoutesAction.StartNavigation -> {
                if (mapboxNavigation.getNavigationRoutes().isEmpty()) {
                    // fetching route if started from free drive
                    fetchAndSetRoute(mapboxNavigation, state.destination) {
                        startNavigation(mapboxNavigation)
                    }
                } else {
                    startNavigation(mapboxNavigation)
                }
            }
            is RoutesAction.StopNavigation -> {
                stopNavigation(mapboxNavigation)
            }

            is RoutesAction.DidStartNavigation -> {
                return state.copy(navigationStarted = true)
            }
            is RoutesAction.DidStopNavigation -> {
                return state.copy(navigationStarted = false)
            }
        }
        return state
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        routeRequestId?.let {
            mapboxNavigation.cancelRouteRequest(it)
        }
    }

    @Suppress("MissingPermission")
    private fun startNavigation(mapboxNavigation: MapboxNavigation) {
        with(mapboxNavigation) {
            // Temporarily trigger replay here
            mapboxReplayer.clearEvents()
            resetTripSession()
            mapboxReplayer.pushRealLocation(navigationOptions.applicationContext, 0.0)
            mapboxReplayer.play()
            startReplayTripSession()
        }
        invoke(RoutesAction.DidStartNavigation)
    }

    private fun stopNavigation(mapboxNavigation: MapboxNavigation) {
        with(mapboxNavigation) {
            // Stop replay here
            mapboxReplayer.clearEvents()
            resetTripSession()
        }
        invoke(RoutesAction.DidStopNavigation)
    }

    private fun shouldReloadRoute(
        oldDestination: Destination?,
        newDestination: Destination?
    ) = oldDestination != newDestination &&
        newDestination != null &&
        navigationStateViewModel.state.value == NavigationState.RoutePreview

    private fun fetchAndSetRoute(
        mapboxNavigation: MapboxNavigation,
        destination: Destination?,
        cb: (() -> Unit)? = null
    ) {
        val from = locationViewModel.lastPoint
        val to = destination?.point
        if (from != null && to != null) {
            val routeOptions = getDefaultOptions(mapboxNavigation, listOf(from, to))
            fetchRoute(routeOptions, mapboxNavigation) {
                cb?.invoke()
            }
        }
    }

    private fun fetchRoute(
        options: RouteOptions,
        mapboxNavigation: MapboxNavigation,
        cb: (() -> Unit)? = null
    ) {
        routeRequestId = mapboxNavigation.requestRoutes(
            options,
            object : NavigationRouterCallback {
                override fun onRoutesReady(
                    routes: List<NavigationRoute>,
                    routerOrigin: RouterOrigin
                ) {
                    mapboxNavigation.setNavigationRoutes(routes)
                    cb?.invoke()
                }

                override fun onFailure(
                    reasons: List<RouterFailure>,
                    routeOptions: RouteOptions
                ) {
                    Log.e(TAG, "Failed to fetch route with reason(s):")
                    reasons.forEach {
                        Log.e(TAG, it.message)
                    }
                }

                override fun onCanceled(routeOptions: RouteOptions, routerOrigin: RouterOrigin) {
                    // no impl
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

    private companion object {
        private val TAG = RoutesViewModel::class.java.simpleName
    }
}
