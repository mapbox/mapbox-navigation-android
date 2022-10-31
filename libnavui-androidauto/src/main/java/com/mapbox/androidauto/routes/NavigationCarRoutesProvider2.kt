package com.mapbox.androidauto.routes

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.core.preview.RoutesPreview
import com.mapbox.navigation.core.preview.RoutesPreviewObserver
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow

/**
 * This will replace [NavigationCarRoutesProvider]
 */
@ExperimentalPreviewMapboxNavigationAPI
@OptIn(ExperimentalCoroutinesApi::class)
class NavigationCarRoutesProvider2 : CarRoutesProvider {

    private val _routesPreview = MutableStateFlow<RoutesPreview?>(null)

    val routesPreview: StateFlow<RoutesPreview?> = _routesPreview.asStateFlow()

    init {
        _routesPreview.value = MapboxNavigationApp.current()?.getRoutesPreview()
    }

    /**
     * Observes the routes set to [MapboxNavigation].
     */
    override val navigationRoutes: Flow<List<NavigationRoute>> = MapboxNavigationApp
        .flowNavigationRoutes()

    private fun MapboxNavigationApp.flowNavigationRoutes(): Flow<List<NavigationRoute>> =
        callbackFlow {
            val routesObserver = RoutesPreviewObserver {
                val navigationRoutes: List<NavigationRoute> = it.routesPreview?.routesList
                    ?: emptyList()
                _routesPreview.value = it.routesPreview
                trySend(navigationRoutes)
            }
            val observer = object : MapboxNavigationObserver {
                override fun onAttached(mapboxNavigation: MapboxNavigation) {
                    mapboxNavigation.registerRoutesPreviewObserver(routesObserver)
                }

                override fun onDetached(mapboxNavigation: MapboxNavigation) {
                    mapboxNavigation.unregisterRoutesPreviewObserver(routesObserver)
                    trySend(emptyList())
                }
            }
            registerObserver(observer)
            awaitClose {
                unregisterObserver(observer)
            }
        }
}
