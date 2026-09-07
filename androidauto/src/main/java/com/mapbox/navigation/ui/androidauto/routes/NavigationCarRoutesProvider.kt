package com.mapbox.navigation.ui.androidauto.routes

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * A version of the [CarRoutesProvider] that uses the routes from [MapboxNavigation].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NavigationCarRoutesProvider : CarRoutesProvider {

    /**
     * Observes the routes set to [MapboxNavigation].
     */
    override val navigationRoutes: Flow<List<NavigationRoute>> = MapboxNavigationApp
        .flowNavigationRoutes()

    private fun MapboxNavigationApp.flowNavigationRoutes(): Flow<List<NavigationRoute>> =
        callbackFlow {
            val routesObserver = RoutesObserver {
                trySend(it.navigationRoutes)
            }
            val observer = object : MapboxNavigationObserver {
                override fun onAttached(mapboxNavigation: MapboxNavigation) {
                    mapboxNavigation.registerRoutesObserver(routesObserver)
                }

                override fun onDetached(mapboxNavigation: MapboxNavigation) {
                    mapboxNavigation.unregisterRoutesObserver(routesObserver)
                    trySend(emptyList())
                }
            }
            registerObserver(observer)
            awaitClose {
                unregisterObserver(observer)
            }
        }
}
