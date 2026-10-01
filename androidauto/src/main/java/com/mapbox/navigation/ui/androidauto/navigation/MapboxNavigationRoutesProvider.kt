@file:OptIn(com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI::class)

package com.mapbox.navigation.ui.androidauto.navigation

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.core.preview.RoutesPreview
import com.mapbox.navigation.core.preview.RoutesPreviewObserver
import com.mapbox.navigation.ui.androidauto.internal.extensions.selectPreviewedRoute
import com.mapbox.navigation.ui.androidauto.internal.extensions.startGuidanceOnPreviewedRoute
import com.mapbox.navigation.ui.androidauto.internal.logAndroidAutoFailure
import com.mapbox.navigation.ui.androidauto.routes.CarRoutesProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Combines preview and active routes so one set of map components can render every navigation
 * state. Active routes take precedence over routes being previewed.
 */
internal class MapboxNavigationRoutesProvider : CarRoutesProvider, MapboxNavigationObserver {

    private var activeRoutes = emptyList<NavigationRoute>()
    private var routesPreview: RoutesPreview? = null
    private val mutableState = MutableStateFlow(MapboxNavigationRoutesState())

    val state: StateFlow<MapboxNavigationRoutesState> = mutableState.asStateFlow()

    override val navigationRoutes = state
        .map { it.routes }
        .distinctUntilChanged()

    private val routesObserver = RoutesObserver { result ->
        activeRoutes = result.navigationRoutes
        updateState()
    }

    private val routesPreviewObserver = RoutesPreviewObserver { result ->
        routesPreview = result.routesPreview
        updateState()
    }

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        mapboxNavigation.registerRoutesObserver(routesObserver)
        mapboxNavigation.registerRoutesPreviewObserver(routesPreviewObserver)
        activeRoutes = mapboxNavigation.getNavigationRoutes()
        routesPreview = mapboxNavigation.getRoutesPreview()
        updateState()
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        mapboxNavigation.unregisterRoutesObserver(routesObserver)
        mapboxNavigation.unregisterRoutesPreviewObserver(routesPreviewObserver)
        activeRoutes = emptyList()
        routesPreview = null
        updateState()
    }

    /**
     * Makes the previewed route with [routeId] the primary previewed route. A click from a
     * template built for an older preview, whose route is no longer previewed, is ignored.
     */
    fun selectRoute(routeId: String) {
        val mapboxNavigation = MapboxNavigationApp.current() ?: run {
            logAndroidAutoFailure("$TAG selectRoute ignored, MapboxNavigation is detached")
            return
        }
        if (!mapboxNavigation.selectPreviewedRoute(routeId)) {
            logAndroidAutoFailure("$TAG selectRoute ignored, route $routeId is not previewed")
        }
    }

    /**
     * Starts active guidance on the previewed route with [routeId], with the other previewed
     * routes as alternatives. A click for a route that is no longer previewed is ignored.
     */
    fun startNavigation(routeId: String) {
        val mapboxNavigation = MapboxNavigationApp.current() ?: run {
            logAndroidAutoFailure("$TAG startNavigation ignored, MapboxNavigation is detached")
            return
        }
        if (!mapboxNavigation.startGuidanceOnPreviewedRoute(routeId)) {
            logAndroidAutoFailure(
                "$TAG startNavigation ignored, route $routeId is not previewed",
            )
        }
    }

    fun clearPreview() {
        MapboxNavigationApp.current()?.setRoutesPreview(emptyList())
    }

    private fun updateState() {
        mutableState.value = when {
            activeRoutes.isNotEmpty() -> MapboxNavigationRoutesState(
                screenState = MapboxNavigationScreenState.ACTIVE_GUIDANCE,
                routes = activeRoutes,
                routesPreview = routesPreview,
            )
            !routesPreview?.routesList.isNullOrEmpty() -> MapboxNavigationRoutesState(
                screenState = MapboxNavigationScreenState.ROUTE_PREVIEW,
                routes = routesPreview?.routesList.orEmpty(),
                routesPreview = routesPreview,
            )
            else -> MapboxNavigationRoutesState()
        }
    }

    private companion object {
        private const val TAG = "MapboxNavigationRoutesProvider"
    }
}

internal enum class MapboxNavigationScreenState {
    FREE_DRIVE,
    ROUTE_PREVIEW,
    ACTIVE_GUIDANCE,
}

internal data class MapboxNavigationRoutesState(
    val screenState: MapboxNavigationScreenState = MapboxNavigationScreenState.FREE_DRIVE,
    val routes: List<NavigationRoute> = emptyList(),
    val routesPreview: RoutesPreview? = null,
)
