package com.mapbox.navigation.ui.androidauto.preview

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.core.preview.RoutesPreviewObserver
import com.mapbox.navigation.ui.androidauto.navigation.CarNavigationCamera
import com.mapbox.navigation.ui.androidauto.routes.CarRoutesProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map

/**
 * Gives the [CarRoutePreviewScreen] the ability to control the selected route rendered
 * by the [CarRouteLineRenderer] and [CarNavigationCamera].
 */
@ExperimentalPreviewMapboxNavigationAPI
class PreviewCarRoutesProvider2 : CarRoutesProvider {

    /**
     * Provides navigation routes and selected route index for route preview.
     */
    val routesPreview = callbackFlow {
        val routesObserver = RoutesPreviewObserver { trySend(it.routesPreview) }
        val observer = object : MapboxNavigationObserver {

            override fun onAttached(mapboxNavigation: MapboxNavigation) {
                mapboxNavigation.registerRoutesPreviewObserver(routesObserver)
            }

            override fun onDetached(mapboxNavigation: MapboxNavigation) {
                mapboxNavigation.unregisterRoutesPreviewObserver(routesObserver)
                trySend(element = null)
            }
        }
        MapboxNavigationApp.registerObserver(observer)
        awaitClose {
            MapboxNavigationApp.unregisterObserver(observer)
        }
    }

    override val navigationRoutes = routesPreview.map { it?.routesList.orEmpty() }

    /**
     * When the route selection changes, update the route selection. This will relay the route
     * selection to other components.
     */
    fun updateSelectedRoute(index: Int) {
        val mapboxNavigation = MapboxNavigationApp.current()!!
        val routesPreview = mapboxNavigation.getRoutesPreview()!!
        mapboxNavigation.changeRoutesPreviewPrimaryRoute(routesPreview.originalRoutesList[index])
    }
}
